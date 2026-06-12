package tect.host.tpl.module.impl.chat.blockedwords;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.migration.ConfigMigrator;

import java.util.List;
import java.util.logging.Logger;

public final class BlockedWordsMigrations {

    private BlockedWordsMigrations() {}

    @Contract("_ -> new")
    public static @NonNull ConfigMigrator create(Logger logger) {
        return new ConfigMigrator(logger, "blockedwords.yml")

            // v0 -> v1
            .addMigration(config -> {
                List<String> message = config.getStringList("block-message");
                if (!message.isEmpty()) {
                    java.util.List<String> existing = new java.util.ArrayList<>(config.getStringList("actions"));
                    for (String line : message) {
                        if (line != null && !line.isBlank()) {
                            existing.add("[MESSAGE] " + line.strip());
                        }
                    }
                    config.set("actions", existing);
                    config.set("block-message", null);
                }
            });
    }
}