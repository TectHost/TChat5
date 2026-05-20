package tect.host.tpl.module.impl.command.nick;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.MessagesManager;
import tect.host.tpl.module.registry.ModuleManager;
import tect.host.tpl.module.ModuleCommand;
import tect.host.tpl.util.CompletionUtil;
import tect.host.tpl.util.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class NickCommand implements ModuleCommand {

    private static final String PERM_SELF = "tchat.command.nick";
    private static final String PERM_OTHERS = "tchat.admin.command.nick.others";
    private static final int MAX_NICK_LEN = 32;

    private final ModuleManager moduleManager;
    private final MessagesManager messagesManager;

    public NickCommand(@NonNull ModuleManager moduleManager, @NonNull MessagesManager messagesManager) {
        this.moduleManager = moduleManager;
        this.messagesManager = messagesManager;
    }

    @Override
    public @NonNull String getName() { return "nick"; }

    @Override
    public void execute(@NonNull CommandSourceStack source, String @NonNull [] args) {
        CommandSender sender = source.getSender();

        if (!Utils.hasPerms(sender, PERM_SELF)) {
            messagesManager.sendMessage(sender, "no-permission");
            return;
        }

        NickModule module = moduleManager.getModule("nick", NickModule.class);
        if (module == null) {
            messagesManager.sendMessage(sender, "nick-module-disabled");
            return;
        }

        if (args.length == 0) {
            messagesManager.sendMessage(sender, "nick-usage");
            return;
        }

        String nickArg = args[0];
        Player target;

        if (args.length >= 2) {
            if (!Utils.hasPerms(sender, PERM_OTHERS)) {
                messagesManager.sendMessage(sender, "no-permission");
                return;
            }
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                messagesManager.sendMessage(sender, "player-not-found", Map.of("%player%", args[1]));
                return;
            }
        } else {
            if (!(sender instanceof Player p)) {
                messagesManager.sendMessage(sender, "nick-console-requires-target");
                return;
            }
            target = p;
        }

        if (nickArg.equalsIgnoreCase("off")) {
            module.removeNick(target);
            messagesManager.sendMessage(sender, "nick-removed", Map.of("%player%", target.getName()));
            return;
        }

        if (nickArg.length() > MAX_NICK_LEN) {
            messagesManager.sendMessage(sender, "nick-too-long", Map.of("%max%", String.valueOf(MAX_NICK_LEN)));
            return;
        }

        if (!isValidNick(nickArg)) {
            messagesManager.sendMessage(sender, "nick-invalid-chars");
            return;
        }

        module.setNick(target, nickArg);
        messagesManager.sendMessage(sender, "nick-set", Map.of("%nick%", nickArg, "%player%", target.getName()));
    }

    private static boolean isValidNick(@NonNull String nick) {
        for (int i = 0; i < nick.length(); i++) {
            char c = nick.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '-' && c != '_') return false;
        }
        return !nick.isBlank();
    }

    @Override
    public @NonNull Collection<String> suggest(@NonNull CommandSourceStack source, String @NonNull [] args) {
        CommandSender sender = source.getSender();

        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("off");
            if (Utils.hasPerms(sender, PERM_OTHERS)) {
                suggestions.addAll(CompletionUtil.filterOnlinePlayers(args[0]));
            }
            return suggestions;
        }

        if (args.length == 2 && Utils.hasPerms(sender, PERM_OTHERS)) {
            return CompletionUtil.filterOnlinePlayers(args[1]);
        }

        return List.of();
    }
}