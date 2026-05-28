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
            })

            // v2 -> v3
            .addMigration(config -> {
                config.set("messages.block-chat-blocked", "<red>The chat is temporarily blocked.</red>");
                config.set("messages.block-chat-enabled", "<green>You have blocked the global chat.</green>");
                config.set("messages.block-chat-disabled", "<green>You have unblocked the global chat.</green>");
                config.set("messages.block-chat-enabled-broadcast", "<dark_gray>[</dark_gray><red>Chat</red><dark_gray>]</dark_gray> <gray>Chat has been blocked by <red>%player%</red>.</gray>");
                config.set("messages.block-chat-disabled-broadcast", "<dark_gray>[</dark_gray><green>Chat</green><dark_gray>]</dark_gray> <gray>Chat has been unblocked by <green>%player%</green>.</gray>");
                config.set("messages.block-chat-already-on", "<yellow>The chat is already blocked.</yellow>");
                config.set("messages.block-chat-already-off", "<yellow>The chat is already unblocked.</yellow>");
                config.set("messages.block-chat-status-on", "<gray>Chat status: <red>Blocked</red></gray>");
                config.set("messages.block-chat-status-off", "<gray>Chat status: <green>Unblocked</green></gray>");
                config.set("messages.block-chat-usage", "<gray>Usage: /blockchat [on|off|toggle|status]</gray>");
                config.set("messages.block-chat-module-disabled", "<red>The block-chat module is not active.</red>");

                config.set("messages.command-player-only", "<red>This command can only be executed by a player.</red>");
                config.set("messages.channel-module-disabled", "<red>The channel module is not enabled.</red>");
                config.set("messages.channel-usage", "<gray>Usage: /channel <join|leave|send|list> [channel] [message]</gray>");
                config.set("messages.channel-join-usage", "<gray>Usage: /channel join <channel></gray>");
                config.set("messages.channel-send-usage", "<gray>Usage: /channel send <channel> <message></gray>");
                config.set("messages.channel-not-found", "<red>Channel <white>%channel%</white> not found.</red>");
                config.set("messages.channel-not-in", "<red>You are not in channel <white>%channel%</white>.</red>");
                config.set("messages.channel-not-in-any", "<red>You are not in any channel.</red>");
                config.set("messages.channel-already-joined", "<yellow>You are already in channel <white>%channel%</white>.</yellow>");
                config.set("messages.channel-full", "<red>Channel <white>%channel%</white> is full.</red>");
                config.set("messages.channel-no-recipients", "<yellow>There are no recipients in channel <white>%channel%</white>.</yellow>");
                config.set("messages.channel-joined", "<green>You joined channel <white>%channel%</white>.</green>");
                config.set("messages.channel-left", "<gray>You left channel <white>%channel%</white>.</gray>");
                config.set("messages.channel-send-success", "<gray>Message sent to <white>%channel%</white>: %message%</gray>");
                config.set("messages.channel-announce-join", "<dark_gray>[</dark_gray><aqua>%channel%</aqua><dark_gray>]</dark_gray> <gray><white>%player%</white> joined the channel.</gray>");
                config.set("messages.channel-announce-leave", "<dark_gray>[</dark_gray><aqua>%channel%</aqua><dark_gray>]</dark_gray> <gray><white>%player%</white> left the channel.</gray>");
                config.set("messages.channel-list-header", "<gold>Available channels:</gold>");
                config.set("messages.channel-list-entry", "<gray>  - <white>%channel%</white></gray>");
                config.set("messages.channel-list-entry-active", "<gray>  - <aqua>%channel%</aqua> <dark_gray>(active)</dark_gray></gray>");
                config.set("messages.channel-list-empty", "<gray>There are no channels available to you.</gray>");

                config.set("messages.command-player-only", "<red>This command can only be executed by a player.</red>");
            });
    }
}