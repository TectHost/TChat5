package tect.host.tpl.config.migration;

import org.jspecify.annotations.NonNull;

import java.util.logging.Logger;

public final class ConfigMigrations {

    private ConfigMigrations() {}

    public static @NonNull ConfigMigrator forConfig(@NonNull Logger logger) {
        return new ConfigMigrator(logger, "config.yml")

            // v0 -> v1
            .addMigrations(config -> config.set("modules.blocked-words", false))

            // v1 -> v2
            .addMigrations(config -> {
                config.set("storage.method", "SQLite");
                config.set("storage.remote.host", "localhost");
                config.set("storage.remote.port", 3306);
                config.set("storage.remote.database", "tchat");
                config.set("storage.remote.username", "root");
                config.set("storage.remote.password", "");

                config.set("modules.anti-cap", false);
                config.set("modules.blocked-commands", false);
                config.set("modules.nick", false);
            });
    }
}