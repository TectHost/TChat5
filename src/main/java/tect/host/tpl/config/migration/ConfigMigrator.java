package tect.host.tpl.config.migration;

import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ConfigMigrator {

    private static final String VERSION_KEY = "config-version";

    private final Logger logger;
    private final String fileName;

    private final List<ConfigMigration> migrations = new ArrayList<>();

    public ConfigMigrator(@NonNull Logger logger, @NonNull String fileName) {
        this.logger = logger;
        this.fileName = fileName;
    }

    /**
     * Applies all pending migrations to the given FileConfiguration
     * If any changes are made, the file will be saved to disk
     *
     * @return true if at least one migration was applied
     */
    public boolean migrate(@NonNull FileConfiguration config, @NonNull File file) {
        int current = config.getInt(VERSION_KEY, 0);
        int target = migrations.size();

        if (current >= target) return false;

        logger.info("[%s] Migrating config from v%d to v%d".formatted(fileName, current, target));

        for (int v = current; v < target; v++) {
            try {
                migrations.get(v).apply(config);
                logger.info("[%s] Applied migration v%d → v%d".formatted(fileName, v, v + 1));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "[%s] Migration v%d → v%d failed, stopping".formatted(fileName, v, v + 1), e);
                break;
            }
            config.set(VERSION_KEY, v + 1);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "[%s] Could not save migrated config".formatted(fileName), e);
        }

        return true;
    }
}