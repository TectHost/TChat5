package tect.host.tpl.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.module.hook.placeholderapi.PlaceholderApiHook;

import java.util.Map;

public final class ColorUtil {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private static final Map<String, String> LEGACY_MAP = Map.ofEntries(
            Map.entry("0", "<black>"), Map.entry("1", "<dark_blue>"),
            Map.entry("2", "<dark_green>"), Map.entry("3", "<dark_aqua>"),
            Map.entry("4", "<dark_red>"), Map.entry("5", "<dark_purple>"),
            Map.entry("6", "<gold>"), Map.entry("7", "<gray>"),
            Map.entry("8", "<dark_gray>"), Map.entry("9", "<blue>"),
            Map.entry("a", "<green>"), Map.entry("b", "<aqua>"),
            Map.entry("c", "<red>"), Map.entry("d", "<light_purple>"),
            Map.entry("e", "<yellow>"), Map.entry("f", "<white>"),
            Map.entry("k", "<obfuscated>"), Map.entry("l", "<bold>"),
            Map.entry("m", "<strikethrough>"), Map.entry("n", "<underlined>"),
            Map.entry("o", "<italic>"), Map.entry("r", "<reset>")
    );

    private ColorUtil() {}

    public static @NonNull Component translate(@NonNull PlaceholderApiHook hook, @Nullable Player player, @NonNull String message) {
        return deserialize(hook.apply(player, message));
    }

    /**
     * Converts legacy & codes to MiniMessage format
     * Use ONLY for trusted external input (e.g. LuckPerms prefixes via PAPI)
     * that may contain legacy codes.
     */
    public static @NonNull String legacyToMini(@NonNull String legacy) {
        return MINI.serialize(LEGACY.deserialize(legacy));
    }

    /**
     * Extracts plain text from a Component, stripping all formatting.
     */
    public static @NonNull String toPlainText(@NonNull Component component) {
        return PLAIN.serialize(component);
    }

    public static @NonNull Component deserialize(@NonNull String miniMessage) {
        return MINI.deserialize(miniMessage);
    }

    public static @NonNull Component deserialize(@NonNull String miniMessage, @NonNull TagResolver tagResolver) {
        return MINI.deserialize(miniMessage, tagResolver);
    }

    /**
     * Converts legacy &-codes to MiniMessage tags in a string that may already
     * contain MiniMessage tags, leaving those tags untouched
     */
    public static @NonNull String legacyToMiniSafe(@NonNull String input) {
        if (input.indexOf('&') == -1 && input.indexOf('<') == -1) return input;

        StringBuilder result = new StringBuilder(input.length());
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == '<') {
                int end = input.indexOf('>', i);
                if (end == -1) { convertLegacy(input, i, input.length(), result); break; }
                result.append(input, i, end + 1);
                i = end + 1;
            } else if (c == '&' && i + 1 < input.length()) {
                String tag = LEGACY_MAP.get(String.valueOf(input.charAt(i + 1)).toLowerCase());
                if (tag != null) { result.append(tag); i += 2; }
                else { result.append(c); i++; }
            } else {
                result.append(c);
                i++;
            }
        }
        return result.toString();
    }

    private static void convertLegacy(@NonNull String input, int from, int to, @NonNull StringBuilder out) {
        int i = from;
        while (i < to) {
            char c = input.charAt(i);
            if (c == '&' && i + 1 < to) {
                String tag = LEGACY_MAP.get(String.valueOf(input.charAt(i + 1)).toLowerCase());
                if (tag != null) { out.append(tag); i += 2; continue; }
            }
            out.append(c);
            i++;
        }
    }
}