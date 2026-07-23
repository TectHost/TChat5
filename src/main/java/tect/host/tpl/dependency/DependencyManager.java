package tect.host.tpl.dependency;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.util.logging.DebugLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Resolves and loads runtime dependencies
 *.
 * Downloads and caches libraries when needed, then loads them for use by any
 * component through an isolated class loader
 */
public final class DependencyManager {

    private static final int DOWNLOAD_THREADS = 2;
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 10;

    private final Path cacheDir;
    private final Path checksumsFile;
    private final Properties checksums;
    private final DependencyDownloader downloader;
    private final IsolatedClassLoader classLoader;
    private final ExecutorService downloadExecutor;
    private final DebugLogger debugLogger;

    public DependencyManager(File dataFolder, ClassLoader parent, DebugLogger debugLogger) {
        this.debugLogger = debugLogger;
        this.downloader = new DependencyDownloader(debugLogger);
        this.classLoader = new IsolatedClassLoader(new URL[0], parent);

        Path librariesDir = new File(dataFolder, "libraries").toPath();
        this.cacheDir = librariesDir.resolve("cache");

        this.checksumsFile = librariesDir.resolve("checksums.properties");
        this.checksums = new Properties();

        try {
            Files.createDirectories(cacheDir);
            loadChecksums();
        } catch (IOException e) {
            throw new RuntimeException("The dependency directories could not be created", e);
        }

        AtomicInteger threadCount = new AtomicInteger(1);
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "TChat-DependencyDownloader-" + threadCount.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
        this.downloadExecutor = Executors.newFixedThreadPool(DOWNLOAD_THREADS, threadFactory);
    }

    public IsolatedClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Resolves and loads the given dependencies
     *.
     * Cached dependencies are loaded immediately, while missing ones are
     * downloaded in the background
     */
    public @NonNull CompletableFuture<Void> loadAll(@NonNull Set<Dependency> dependencies) {
        CompletableFuture<?>[] futures = dependencies.stream()
                .map(this::load)
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures);
    }

    private CompletableFuture<Void> load(Dependency dependency) {
        Path jar = cachedPath(dependency);

        if (Files.exists(jar)) {
            link(jar);
            debugLogger.info("[Dependencies] %s loaded from local cache".formatted(dependency.artifactId()));
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture
                .runAsync(() -> obtain(dependency, jar), downloadExecutor)
                .thenAccept(_ -> link(jar));
    }

    private void obtain(Dependency dependency, Path jar) {
        try {
            String key = checksumKey(dependency);
            String expectedChecksum = dependency.checksum() != null ? dependency.checksum() : checksums.getProperty(key);

            DependencyDownloader.DownloadResult result = downloader.download(dependency, expectedChecksum);

            if (expectedChecksum == null) {
                pinChecksum(key, result.sha256());
                debugLogger.warn("[Dependencies] %s set for the first time (trust-on-first-use), sha256=%s".formatted(dependency.artifactId(), result.sha256()));
            }

            Path tempFile = cacheDir.resolve(jar.getFileName().toString() + ".tmp");
            Files.write(tempFile, result.bytes());
            Files.move(tempFile, jar, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            debugLogger.info("[Dependencies] %s prepared in %s".formatted(dependency.artifactId(), jar.getFileName()));
        } catch (IOException e) {
            throw new RuntimeException("It was not possible to prepare the dependency " + dependency.artifactId(), e);
        }
    }

    private void link(@NonNull Path jar) {
        try {
            classLoader.addURL(jar.toUri().toURL());
        } catch (IOException e) {
            throw new RuntimeException("The JAR file could not be loaded " + jar, e);
        }
    }

    private @NonNull Path cachedPath(@NonNull Dependency dependency) {
        return cacheDir.resolve(dependency.artifactId() + "-" + dependency.version() + ".jar");
    }

    private @NonNull String checksumKey(@NonNull Dependency dependency) {
        return dependency.artifactId() + "-" + dependency.version();
    }

    private void loadChecksums() throws IOException {
        if (!Files.exists(checksumsFile)) {
            return;
        }
        try (InputStream in = Files.newInputStream(checksumsFile)) {
            checksums.load(in);
        }
    }

    /** Synchronized: Multiple concurrent downloads can set new checksums at the same time */
    private synchronized void pinChecksum(String key, String sha256) {
        checksums.setProperty(key, sha256);
        try (OutputStream out = Files.newOutputStream(checksumsFile)) {
            checksums.store(out, "SHA-256 of the dependencies downloaded at runtime. Do not edit this manually unless you know what you're doing");
        } catch (IOException e) {
            debugLogger.warn("[Dependencies] Could not save checksums.properties: " + e.getMessage());
        }
    }

    /** Waits for pending downloads to finish before closing the class loader */
    public void shutdown() {
        downloadExecutor.shutdown();
        try {
            if (!downloadExecutor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                downloadExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            downloadExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        try {
            classLoader.close();
        } catch (IOException e) {
            debugLogger.warn("[Dependencies] The dependency classloader could not be closed: " + e.getMessage());
        }
    }
}