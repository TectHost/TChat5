package tect.host.tpl.config.migration;

import org.jspecify.annotations.NonNull;

import java.util.logging.Logger;

public final class ConfigMigrations {

    private ConfigMigrations() {}

    public static @NonNull ConfigMigrator forConfig(@NonNull Logger logger) {
        return new ConfigMigrator(logger, "config.yml")

            // v0 -> v1
            .addMigrations(config -> {
              if (!config.isSet("modules.blocked-words")) {
                config.set("modules.blocked-words", false);
              }
            });
    }
}