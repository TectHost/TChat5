package tect.host.tpl.module.impl.chat.anticap;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.migration.ConfigMigrator;

import java.util.logging.Logger;

public final class AntiCapMigrations {

    private AntiCapMigrations() {}

    @Contract("_ -> new")
    public static @NonNull ConfigMigrator create(Logger logger) {
        return new ConfigMigrator(logger, "anticap.yml");

        // v0 -> v1:
        // .addMigration(config -> {
        //     config.set("path", "default");
        // })
    }
}