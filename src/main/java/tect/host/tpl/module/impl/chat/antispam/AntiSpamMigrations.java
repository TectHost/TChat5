package tect.host.tpl.module.impl.chat.antispam;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.migration.ConfigMigrator;

import java.util.logging.Logger;

public final class AntiSpamMigrations {

    private AntiSpamMigrations() {}

    @Contract("_ -> new")
    public static @NonNull ConfigMigrator create(@NonNull Logger logger) {
        return new ConfigMigrator(logger, "antispam.yml");

        // v0 -> v1
        //.addMigration(config -> {
        //    config.set("path", "default");
        //})
    }
}