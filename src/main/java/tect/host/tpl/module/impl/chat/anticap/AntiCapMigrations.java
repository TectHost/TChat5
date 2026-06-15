package tect.host.tpl.module.impl.chat.anticap;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.migration.ConfigMigrator;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class AntiCapMigrations {

    private AntiCapMigrations() {}

    @Contract("_ -> new")
    public static @NonNull ConfigMigrator create(Logger logger) {
        return new ConfigMigrator(logger, "anticap.yml")

            // v0 -> v1:
            .addMigration(config -> {
                List<String> message = config.getStringList("message");
                if (!message.isEmpty()) {
                    List<String> existing = new ArrayList<>(config.getStringList("actions"));
                    for (String line : message) {
                        if (line != null && !line.isBlank()) {
                            existing.add("[MESSAGE] " + line.strip());
                        }
                    }
                    config.set("actions", existing);
                    config.set("message", null);
                }
            });
    }
}