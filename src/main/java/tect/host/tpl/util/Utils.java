package tect.host.tpl.util;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public class Utils {

    private Utils() {}

    public static boolean hasPerms(@NonNull Player player, String perm) {
        return player.hasPermission("tchat.admin") || player.hasPermission(perm);
    }

    public static boolean hasPerms(@NonNull CommandSender sender, String perm) {
        return sender.hasPermission("tchat.admin") || sender.hasPermission(perm);
    }
}
