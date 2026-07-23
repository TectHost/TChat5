package tect.host.tpl.dependency;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.util.logging.DebugLogger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/** Downloads a dependency jar, verifying its checksum when one is provided */
public final class DependencyDownloader {

    private final HttpClient httpClient;
    private final DebugLogger debugLogger;

    public DependencyDownloader(DebugLogger debugLogger) {
        this.debugLogger = debugLogger;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Downloads a dependency jar from configured repositories
     *.
     * expectedChecksum, Expected SHA-256 checksum in hex, or null
     * when using trust-on-first-use
     */
    @Contract("_, _ -> new")
    public @NonNull DownloadResult download(Dependency dependency, String expectedChecksum) throws IOException {
        IOException lastError = null;

        for (DependencyRepository repository : DependencyRepository.values()) {
            String url = repository.resolveUrl(dependency);

            try {
                byte[] bytes = fetch(url);
                String hash = sha256(bytes);

                if (expectedChecksum != null && !expectedChecksum.equalsIgnoreCase(hash)) {
                    String message = "Invalid checksum for %s in %s (expected %s, found %s)".formatted(dependency.artifactId(), repository, expectedChecksum, hash);
                    debugLogger.warn("[Dependencies] %s, testing the next repository".formatted(message));
                    lastError = new IOException(message);
                    continue;
                }

                debugLogger.info("[Dependencies] %s downloaded from %s (sha256=%s)".formatted(dependency.artifactId(), repository, hash));
                return new DownloadResult(bytes, hash);
            } catch (IOException e) {
                lastError = e;
            }
        }

        String reason = expectedChecksum != null
                ? "no repository returned a JAR file that matched the specified checksum (possible tampering or version change without updating the PIN)"
                : "none of the configured repositories responded correctly";

        throw new IOException("The download failed " + dependency.artifactId() + ": " + reason, lastError);
    }

    public record DownloadResult(byte[] bytes, String sha256) {
    }

    private byte[] fetch(String url) throws IOException {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                throw new IOException("HTTP %d in %s".formatted(response.statusCode(), url));
            }

            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download Interrupted: " + url, e);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}