package tect.host.tpl.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.TChat;
import tect.host.tpl.util.CompletionUtil;
import tect.host.tpl.util.Utils;

import java.util.Collection;
import java.util.List;

public final class TChatCommand implements BasicCommand {

    private static final String RELOAD_PERMISSION = "tchat.admin.command.reload";

    private final TChat plugin;

    public TChatCommand(TChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(@NonNull CommandSourceStack source, String @NonNull [] args) {
        CommandSender sender = source.getSender();

        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            plugin.getMessagesManager().sendMessage(sender, "unknown-subcommand");
            return;
        }

        if (!Utils.hasPerms(sender, RELOAD_PERMISSION)) {
            plugin.getMessagesManager().sendMessage(sender, "no-permission");
            return;
        }

        plugin.getMessagesManager().sendMessage(sender, "reload-start");

        try {
            plugin.reloadPluginState();
            plugin.getMessagesManager().sendMessage(sender, "reload-success");
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed to reload TChat: " + ex.getMessage());
            plugin.getMessagesManager().sendMessage(sender, "reload-failure");
        }
    }

    @Override
    public @NonNull Collection<String> suggest(@NonNull CommandSourceStack source, String @NonNull [] args) {
        if (args.length <= 1) {
            return CompletionUtil.filterFrom(List.of("reload"), args.length == 1 ? args[0] : "");
        }
        return List.of();
    }
}