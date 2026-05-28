package tect.host.tpl.module.impl.chat.channel;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.migration.ConfigMigrator;

import java.util.logging.Logger;

public final class ChannelMigrations {

    private ChannelMigrations() {}

    @Contract("_ -> new")
    public static @NonNull ConfigMigrator create(Logger logger) {
        return new ConfigMigrator(logger, "channels.yml");

        // v0 -> v1 example:
        //.addMigration(config -> {
        //    config.set("path", "default");
        //})
    }
}