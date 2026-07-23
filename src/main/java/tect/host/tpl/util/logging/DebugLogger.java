package tect.host.tpl.util.logging;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigManager;
import tect.host.tpl.util.Utils;

import java.util.logging.Logger;

public final class DebugLogger {

    private static final String DEBUG_PATH = "debug";

    private final Logger logger;
    private final ConfigManager configManager;
    private volatile boolean debugEnabled;

    private DebugLogger(@NonNull Logger logger, @NonNull ConfigManager configManager) {
        this.logger = logger;
        this.configManager = configManager;
        this.debugEnabled = configManager.getBoolean(DEBUG_PATH);
    }

    public static @NonNull DebugLogger of(@NonNull Logger logger, @NonNull ConfigManager configManager) {
        return new DebugLogger(logger, configManager);
    }

    public void refresh() {
        this.debugEnabled = configManager.getBoolean(DEBUG_PATH);
    }

    public boolean isEnabled() {
        return debugEnabled;
    }

    public void debug(@NonNull String message) {
        if (debugEnabled) Utils.log(logger, "INFO", "[DEBUG] " + message);
    }

    public void info(@NonNull String message) {
        if (debugEnabled) Utils.log(logger, "INFO", message);
    }

    public void warn(@NonNull String message) {
        if (debugEnabled) Utils.log(logger, "WARNING", message);
    }

    public void severe(@NonNull String message) {
        if (debugEnabled) Utils.log(logger, "SEVERE", message);
    }
}