package tect.host.tpl.util;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.TChat;

import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public final class Utils {

    private static final String ADMIN_PERM = "tchat.admin";

    private Utils() {}

    public static boolean hasPerms(@NonNull Player player, String perm) {
        return player.hasPermission(ADMIN_PERM) || player.hasPermission(perm);
    }

    public static boolean hasPerms(@NonNull CommandSender sender, String perm) {
        return sender.hasPermission(ADMIN_PERM) || sender.hasPermission(perm);
    }

    public static void log(@NonNull Logger logger, @NonNull String level, String message) {
        switch (level) {
            case "WARNING", "WARN" -> logger.warning(message);
            case "SEVERE", "ERROR" -> logger.severe(message);
            case "FINE", "DEBUG" -> logger.fine(message);
            default -> logger.info(message);
        }
    }

    public static @NonNull String getPluginVersion(@NonNull TChat plugin) {
        return plugin.getPluginMeta().getVersion();
    }

    public static @NonNull List<String> getPluginAuthors(@NonNull TChat plugin) {
        return plugin.getPluginMeta().getAuthors();
    }

    public static double parseDouble(@NonNull String value, @NonNull String fullArg, @NonNull Logger log) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.warning("ActionExecutor: expected number, got '%s' in '%s'".formatted(value, fullArg));
            return Double.NaN;
        }
    }

    public static int parseInt(@NonNull String value, @NonNull String fullArg, @NonNull Logger log) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warning("ActionExecutor: expected integer, got '%s' in '%s'".formatted(value, fullArg));
            return -1;
        }
    }

    public static @Nullable Material parseMaterial(@NonNull String name, @NonNull Logger log) {
        Material mat = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
        if (mat == null) log.warning("ActionExecutor [INVENTORY]: unknown material '%s'".formatted(name));
        return mat;
    }
}
