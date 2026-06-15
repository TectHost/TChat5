package tect.host.tpl.module.impl.command.invsee;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.MessagesManager;
import tect.host.tpl.module.ModuleCommand;
import tect.host.tpl.module.ModuleMenu;
import tect.host.tpl.module.registry.ModuleManager;
import tect.host.tpl.util.CompletionUtil;
import tect.host.tpl.util.Utils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class InvSeeCommand implements ModuleCommand {

    private static final String PERM = "tchat.admin.command.invsee";

    private final ModuleManager moduleManager;
    private final MessagesManager messagesManager;

    public InvSeeCommand(@NonNull ModuleManager moduleManager, @NonNull MessagesManager messagesManager) {
        this.moduleManager = moduleManager;
        this.messagesManager = messagesManager;
    }

    @Override
    public @NonNull String getName() { return "invsee"; }

    @Override
    public void execute(@NonNull CommandSourceStack source, String @NonNull [] args) {
        CommandSender sender = source.getSender();

        if (!Utils.hasPerms(sender, PERM)) {
            messagesManager.sendMessage(sender, "no-permission");
            return;
        }

        if (!(sender instanceof Player viewer)) {
            messagesManager.sendMessage(sender, "command-player-only");
            return;
        }

        if (args.length == 0) {
            messagesManager.sendMessage(sender, "invsee-usage");
            return;
        }

        ModuleMenu menu = moduleManager.getMenu("invsee");
        if (menu == null) {
            messagesManager.sendMessage(sender, "invsee-module-disabled");
            return;
        }

        // Validate target exists before delegating, so we can send a proper message
        if (org.bukkit.Bukkit.getPlayerExact(args[0]) == null) {
            messagesManager.sendMessage(sender, "player-not-found", Map.of("%player%", args[0]));
            return;
        }

        menu.open(viewer, args);
    }

    @Override
    public @NonNull Collection<String> suggest(@NonNull CommandSourceStack source, String @NonNull [] args) {
        if (args.length == 1) return CompletionUtil.filterOnlinePlayers(args[0]);
        return List.of();
    }
}