package tect.host.tpl.module.impl.broadcast.autobroadcast;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.migration.ConfigMigrator;

import java.util.logging.Logger;

public final class AutoBroadcastMigrations {

    private AutoBroadcastMigrations() {}

    @Contract("_ -> new")
    public static @NonNull ConfigMigrator create(Logger logger) {
        return new ConfigMigrator(logger, "autobroadcast.yml");

        // v0 -> v1:
        // .addMigration(config -> {
        //     config.set("path", "default");
        // })
    }
}