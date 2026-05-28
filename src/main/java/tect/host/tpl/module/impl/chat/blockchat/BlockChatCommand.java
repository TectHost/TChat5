package tect.host.tpl.module.impl.chat.blockchat;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.MessagesManager;
import tect.host.tpl.module.ModuleCommand;
import tect.host.tpl.module.registry.ModuleManager;
import tect.host.tpl.util.CompletionUtil;
import tect.host.tpl.util.Utils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class BlockChatCommand implements ModuleCommand {

    private static final String PERMISSION = "tchat.admin.command.blockchat";

    private final ModuleManager moduleManager;
    private final MessagesManager messagesManager;

    public BlockChatCommand(@NonNull ModuleManager moduleManager, @NonNull MessagesManager messagesManager) {
        this.moduleManager = moduleManager;
        this.messagesManager = messagesManager;
    }

    @Override
    public @NonNull String getName() { return "blockchat"; }

    @Contract(value = " -> new", pure = true)
    @Override
    public @NonNull @Unmodifiable List<String> getAliases() { return List.of("chatblock", "muteall"); }

    @Override
    public void execute(@NonNull CommandSourceStack source, String @NonNull [] args) {
        CommandSender sender = source.getSender();

        if (!Utils.hasPerms(sender, PERMISSION)) {
            messagesManager.sendMessage(sender, "no-permission");
            return;
        }

        BlockChatModule module = moduleManager.getModule(BlockChatModule.ID, BlockChatModule.class);
        if (module == null) {
            messagesManager.sendMessage(sender, "block-chat-module-disabled");
            return;
        }

        String sub = args.length > 0 ? args[0].toLowerCase() : "toggle";

        switch (sub) {
            case "on" -> {
                if (module.isBlocked()) {
                    messagesManager.sendMessage(sender, "block-chat-already-on");
                    return;
                }
                module.setBlocked(true);
                broadcastStatus(sender, true);
            }
            case "off" -> {
                if (!module.isBlocked()) {
                    messagesManager.sendMessage(sender, "block-chat-already-off");
                    return;
                }
                module.setBlocked(false);
                broadcastStatus(sender, false);
            }
            case "toggle" -> {
                module.toggle();
                broadcastStatus(sender, module.isBlocked());
            }
            case "status" -> {
                String key = module.isBlocked() ? "block-chat-status-on" : "block-chat-status-off";
                messagesManager.sendMessage(sender, key);
            }
            default -> messagesManager.sendMessage(sender, "block-chat-usage");
        }
    }

    @Override
    public @NonNull Collection<String> suggest(@NonNull CommandSourceStack source, String @NonNull [] args) {
        if (!Utils.hasPerms(source.getSender(), PERMISSION)) return List.of();
        if (args.length <= 1) {
            return CompletionUtil.filterFrom(List.of("on", "off", "toggle", "status"), args.length == 1 ? args[0] : "");
        }
        return List.of();
    }

    private void broadcastStatus(@NonNull CommandSender sender, boolean isBlocked) {
        String senderName = sender.getName();
        String broadcastKey = isBlocked ? "block-chat-enabled-broadcast" : "block-chat-disabled-broadcast";

        String ownKey = isBlocked ? "block-chat-enabled" : "block-chat-disabled";
        messagesManager.sendMessage(sender, ownKey);

        for (var player : moduleManager.getModuleContext().getOnlinePlayers()) {
            messagesManager.sendMessage(player, broadcastKey, Map.of("%player%", senderName));
        }
    }
}