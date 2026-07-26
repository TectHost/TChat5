package tect.host.tpl.module.impl.chat.chatdelete;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.MessagesManager;
import tect.host.tpl.module.registry.ModuleManager;
import tect.host.tpl.module.ModuleCommand;
import tect.host.tpl.util.Utils;

import java.util.List;
import java.util.Map;

public final class ChatDeleteCommand implements ModuleCommand {

    private final ModuleManager moduleManager;
    private final MessagesManager messagesManager;

    public ChatDeleteCommand(@NonNull ModuleManager moduleManager, @NonNull MessagesManager messagesManager) {
        this.moduleManager = moduleManager;
        this.messagesManager = messagesManager;
    }

    @Override
    public @NonNull String getName() { return "chatdelete"; }

    @Contract(value = " -> new", pure = true)
    @Override
    public @NonNull @Unmodifiable List<String> getAliases() { return List.of("cdel"); }

    @Override
    public void execute(@NonNull CommandSourceStack source, String @NonNull [] args) {
        CommandSender sender = source.getSender();

        if (!Utils.hasPerms(sender, ChatDeleteModule.PERMISSION)) {
            messagesManager.sendMessage(sender, "no-permission");
            return;
        }

        if (args.length < 1) {
            messagesManager.sendMessage(sender, "chat-delete-usage");
            return;
        }

        ChatDeleteModule module = moduleManager.getModule("chat-delete", ChatDeleteModule.class);
        if (module == null) {
            messagesManager.sendMessage(sender, "chat-delete-module-disabled");
            return;
        }

        switch (module.deleteMessage(args[0])) {
            case DELETED -> messagesManager.sendMessage(sender, "chat-delete-success", Map.of("%id%", args[0]));
            case UNSIGNED -> messagesManager.sendMessage(sender, "chat-delete-unsigned");
            case NOT_FOUND -> messagesManager.sendMessage(sender, "chat-delete-not-found");
        }
    }
}