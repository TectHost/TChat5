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

public final class ColorUtil {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private ColorUtil() {}

    private static @Nullable String legacyTag(char c) {
        return switch (Character.toLowerCase(c)) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            case 'r' -> "<reset>";
            default -> null;
        };
    }

    public static @NonNull Component translate(@NonNull PlaceholderApiHook hook, @Nullable Player player, @NonNull String message) {
        return deserialize(hook.apply(player, message));
    }

    /**
     * Converts legacy & codes to MiniMessage format
     * Use ONLY for trusted external input that may contain legacy codes.
     */
    public static @NonNull String legacyToMini(@NonNull String legacy) {
        return MINI.serialize(LEGACY.deserialize(legacy));
    }

    /** Extracts plain text from a Component, stripping all formatting. */
    public static @NonNull String toPlainText(@NonNull Component component) {
        return PLAIN.serialize(component);
    }

    public static @NonNull Component deserialize(@NonNull String miniMessage) {
        return MINI.deserialize(miniMessage);
    }

    public static @NonNull Component deserialize(@NonNull String miniMessage, @NonNull TagResolver tagResolver) {
        return MINI.deserialize(miniMessage, tagResolver);
    }

    public static @NonNull Component deserialize(@NonNull String miniMessage, @NonNull TagResolver... resolvers) {
        return MINI.deserialize(miniMessage, resolvers);
    }

    public static @NonNull Component render(@NonNull PlaceholderApiHook papi, @NonNull Player player, @NonNull String text) {
        return ColorUtil.deserialize(ColorUtil.legacyToMiniSafe(papi.apply(player, text)));
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
                String tag = legacyTag(input.charAt(i + 1));
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
                String tag = legacyTag(input.charAt(i + 1));
                if (tag != null) { out.append(tag); i += 2; continue; }
            }
            out.append(c);
            i++;
        }
    }
}