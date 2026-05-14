package tect.host.tpl.module.impl.chat.blockedwords;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.MessagesManager;
import tect.host.tpl.manager.ModuleManager;
import tect.host.tpl.module.ModuleCommand;
import tect.host.tpl.util.Utils;

import java.util.Map;
import java.util.Set;

public final class BlockedWordsCommand implements ModuleCommand {

    private static final String PERMISSION = "tchat.admin.blockedwords";

    private final ModuleManager moduleManager;
    private final MessagesManager messagesManager;

    public BlockedWordsCommand(@NonNull ModuleManager moduleManager, @NonNull MessagesManager messagesManager) {
        this.moduleManager = moduleManager;
        this.messagesManager = messagesManager;
    }

    @Override
    public @NonNull String getName() { return "blockedwords"; }

    @Override
    public void execute(@NonNull CommandSourceStack source, String @NonNull [] args) {
        CommandSender sender = source.getSender();

        if (!Utils.hasPerms(sender, PERMISSION)) {
            messagesManager.sendMessage(sender, "no-permission");
            return;
        }

        BlockedWordsModule module = moduleManager.getModule("blocked-words", BlockedWordsModule.class);
        if (module == null) {
            messagesManager.sendMessage(sender, "blocked-words-module-disabled");
            return;
        }

        if (args.length == 0) {
            messagesManager.sendMessage(sender, "blocked-words-usage");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (args.length < 2) { messagesManager.sendMessage(sender, "blocked-words-usage"); return; }
                String word = args[1];
                if (module.addWord(word)) {
                    messagesManager.sendMessage(sender, "blocked-words-add-success", Map.of("%word%", word));
                } else {
                    messagesManager.sendMessage(sender, "blocked-words-add-duplicate");
                }
            }
            case "remove" -> {
                if (args.length < 2) { messagesManager.sendMessage(sender, "blocked-words-usage"); return; }
                String word = args[1];
                if (module.removeWord(word)) {
                    messagesManager.sendMessage(sender, "blocked-words-remove-success", Map.of("%word%", word));
                } else {
                    messagesManager.sendMessage(sender, "blocked-words-remove-not-found");
                }
            }
            case "list" -> {
                Set<String> words = module.getWords();
                if (words.isEmpty()) {
                    messagesManager.sendMessage(sender, "blocked-words-list-empty");
                } else {
                    messagesManager.sendMessage(sender, "blocked-words-list", Map.of(
                            "%count%", String.valueOf(words.size()),
                            "%words%", String.join(", ", words)
                    ));
                }
            }
            default -> messagesManager.sendMessage(sender, "blocked-words-usage");
        }
    }
}