package tect.host.tpl.config.migration;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.logging.Logger;

public final class LangMigrations {

    private LangMigrations() {}

    @Contract("_, _ -> new")
    public static @NonNull ConfigMigrator create(@NonNull Logger logger, String langFileName) {
        return new ConfigMigrator(logger, langFileName)

            // v0 -> v1
            .addMigration(config -> {
                if (!config.isSet("blocked-words-module-disabled")) {
                    config.set("blocked-words-module-disabled", "<red>The blocked-words module is not enabled.</red>");
                }

                if (!config.isSet("blocked-words-usage")) {
                    config.set("blocked-words-usage", "<gray>Usage: /blockedwords <add|remove|list> [word]</gray>");
                }

                if (!config.isSet("blocked-words-add-success")) {
                    config.set("blocked-words-add-success", "<green>Word added: <white>%word%</white></green>");
                }

                if (!config.isSet("blocked-words-add-duplicate")) {
                    config.set("blocked-words-add-duplicate", "<yellow>That word is already blocked.</yellow>");
                }

                if (!config.isSet("blocked-words-remove-success")) {
                    config.set("blocked-words-remove-success", "<green>Word removed: <white>%word%</white></green>");
                }

                if (!config.isSet("blocked-words-remove-not-found")) {
                    config.set("blocked-words-remove-not-found", "<yellow>That word is not in the list.</yellow>");
                }

                if (!config.isSet("blocked-words-list-empty")) {
                    config.set("blocked-words-list-empty", "<gray>No words are currently blocked.</gray>");
                }

                if (!config.isSet("blocked-words-list")) {
                    config.set("blocked-words-list", "<gold>Blocked words (%count%): <white>%words%</white></gold>");
                }
            });
    }
}