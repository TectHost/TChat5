package tect.host.tpl.module.impl.join.notify;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.module.SchedulerAccess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public final class UpdateChecker {

    private static final String RESOURCE_URL = "https://api.spigotmc.org/legacy/update.php?resource=111858";
    private static final String DOWNLOAD_URL = "https://www.spigotmc.org/resources/111858/";
    private static final int TIMEOUT_MS = 5000;

    private static volatile @Nullable String latestVersion = null;
    private static volatile @Nullable String currentVersion = null;

    private UpdateChecker() {}

    @SuppressWarnings("ConstantConditions")
    public static void check(@NonNull SchedulerAccess scheduler, @NonNull Logger logger, @NonNull String pluginVersion) {
        currentVersion = pluginVersion;

        scheduler.runAsync(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) URI.create(RESOURCE_URL).toURL().openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(TIMEOUT_MS);
                connection.setReadTimeout(TIMEOUT_MS);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent", "TChat-UpdateChecker");

                int status = connection.getResponseCode();
                if (status < 200 || status >= 300) {
                    logger.warning("Update check failed, HTTP %d".formatted(status));
                    return;
                }

                String fetched;
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    fetched = reader.readLine();
                }

                if (fetched == null || fetched.isBlank()) {
                    logger.warning("Update check failed, empty response from Spigot.");
                    return;
                }

                latestVersion = fetched.trim();

                if (latestVersion.equalsIgnoreCase(currentVersion)) {
                    logger.info("TChat is up to date (%s).".formatted(currentVersion));
                } else {
                    logger.warning("A new version of TChat is available: %s (you have %s)".formatted(latestVersion, currentVersion));
                    logger.warning("Download it at: %s".formatted(DOWNLOAD_URL));
                }

            } catch (IOException e) {
                logger.warning("Update check failed: %s".formatted(e.getMessage()));
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    /**
     * Returns true if an update is available
     * Returns null if the check hasn't completed yet
     */
    @SuppressWarnings("ConstantConditions")
    public static @Nullable Boolean isOutdated() {
        if (latestVersion == null || currentVersion == null) return null;
        return !latestVersion.equalsIgnoreCase(currentVersion);
    }

    public static @Nullable String getLatestVersion() { return latestVersion; }
    public static @Nullable String getCurrentVersion() { return currentVersion; }
}