package tect.host.tpl.module.impl.chat.blockedwords;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.migration.ConfigMigrator;
import java.util.logging.Logger;

public final class BlockedWordsMigrations {

    private BlockedWordsMigrations() {}

    @Contract("_ -> new")
    public static @NonNull ConfigMigrator create(Logger logger) {
        return new ConfigMigrator(logger, "blockedwords.yml");

        // v0 -> v1
        //.addMigration(config -> {
        //    if (!config.isSet("blocked-words.aternos")) {
        //        config.set("blocked-words.aternos", "&f");
        //    }
        //})
    }
}