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
                    config.set("blocked-words-module-disabled", "<red>The blocked-words module is not enabled.</red>");
                    config.set("blocked-words-usage", "<gray>Usage: /blockedwords <add|remove|list> [word]</gray>");
                    config.set("blocked-words-add-success", "<green>Word added: <white>%word%</white></green>");
                    config.set("blocked-words-add-duplicate", "<yellow>That word is already blocked.</yellow>");
                    config.set("blocked-words-remove-success", "<green>Word removed: <white>%word%</white></green>");
                    config.set("blocked-words-remove-not-found", "<yellow>That word is not in the list.</yellow>");
                    config.set("blocked-words-list-empty", "<gray>No words are currently blocked.</gray>");
                    config.set("blocked-words-list", "<gold>Blocked words (%count%): <white>%words%</white></gold>");
                })

                // v1 -> v2
                .addMigration(config -> {
                    config.set("messages.nick-module-disabled", "<red>The nick module is not enabled.</red>");
                    config.set("messages.nick-usage", "<gray>Usage: /nick <nickname|off> [player]</gray>");
                    config.set("messages.player-not-found", "<red>Player <white>%player%</white> not found.</red>");
                    config.set("messages.nick-console-requires-target", "<red>Console must specify a target player: /nick <nickname> <player></red>");
                    config.set("messages.nick-removed", "<green>Nickname removed for <white>%player%</white>.</green>");
                    config.set("messages.nick-too-long", "<red>That nickname is too long! Maximum allowed is <white>%max%</white> characters.</red>");
                    config.set("messages.nick-invalid-chars", "<red>That nickname contains invalid characters. Use only letters, numbers, dashes, and underscores.</red>");
                    config.set("messages.nick-set", "<green>Nickname for <white>%player%</white> set to <white>%nick%</white>.</green>");
                });
    }
}