package tect.host.tpl.module.impl.command.blockedcommands;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.migration.ConfigMigrator;

import java.util.logging.Logger;

public final class BlockedCommandsMigrations {

    private BlockedCommandsMigrations() {}

    @Contract("_ -> new")
    public static @NonNull ConfigMigrator create(Logger logger) {
        return new ConfigMigrator(logger, "blockedcommands.yml");

        // v0 -> v1:
        // .addMigration(config -> {
        //     config.set("path", "default");
        // })
    }
}