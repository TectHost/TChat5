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
            })

            // v2 -> v3
            .addMigrations(config -> {
                config.set("modules.auto-broadcast", false);
                config.set("modules.block-chat", false);
                config.set("modules.channels", false);
            })

            // v3 -> v4
            .addMigrations(config -> {
                config.set("modules.worlds", false);
                config.set("modules.chat-bridge", false);
            })

            // v4 -> v5
            .addMigrations(config -> {
                config.set("modules.invsee", false);
                config.set("modules.chat-placeholders", false);
                config.set("modules.anti-advertising", false);
           })

           // v5 -> v6
           .addMigrations(config -> {
                config.set("modules.custom-commands", false);
                config.set("debug", false);
                config.set("modules.anti-spam", false);
                config.set("modules.chat-cooldown", false);
                config.set("modules.command-cooldown", false);
                config.set("modules.grammar", false);
                config.set("modules.anti-unicode", false);
                config.set("storage.remote.useSSL", false);
                config.set("modules.clickable-links", false);
           })

            // v6 -> v7
               .addMigrations(config -> {
                config.set("modules.chat-delete", false);
            });
    }
}