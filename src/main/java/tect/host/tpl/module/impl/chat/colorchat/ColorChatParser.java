package tect.host.tpl.module.impl.chat.colorchat;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stateless parser that sanitizes a raw player chat string before MiniMessage parsing
 *.
 * Two-pass strategy (both passes are single regex sweeps):
 *.
 *  1. Legacy codes (&X):
 *    Allowed -> translated inline to the canonical MiniMessage tag (e.g. &c → <red>)
 *    Denied -> removed entirely
 *.
 *  2. MiniMessage tags (<tag>, <tag:args>, </tag>):
 *    In BLOCKED_TAGS -> always removed, no permission can grant them
 *    Allowed by permission -> kept as-is
 *    Unknown / denied -> removed entirely
 */
public final class ColorChatParser {

    // &[color/format code] - case-insensitive
    private static final Pattern LEGACY_PATTERN = Pattern.compile("&([0-9a-fA-Fk-oK-OrR])");

    // <tag>, <tag:args…>, </tag> - captures (1)=closing slash, (2)=tag name
    private static final Pattern MINI_PATTERN = Pattern.compile("<(/?)(#[0-9a-fA-F]{6}|[a-zA-Z_][a-zA-Z0-9_]*)(?::[^>]*)?>", Pattern.CASE_INSENSITIVE);

    // Strips all <tag> / </tag> tokens - used only for the blank-message check
    private static final Pattern TAG_STRIP_PATTERN = Pattern.compile("<[^>]*>");

    private ColorChatParser() {}

    /**
     * Returns the sanitized string safe to pass directly into {@code MiniMessage.deserialize()}
     * Legacy codes are translated to MiniMessage tags; disallowed and blocked tags are removed
     */
    public static @NonNull String sanitize(@NonNull Player player, @NonNull String raw) {
        String result = processLegacy(player, raw);
        return processMiniTags(player, result);
    }

    /**
     * Returns {@code true} if the string contains any character that could be
     * a color/format marker. Used as a cheap pre-check to skip sanitize() on
     * plain messages (the common case on most servers)
     */
    public static boolean hasMarkup(@NonNull String raw) {
        return raw.indexOf('&') >= 0 || raw.indexOf('<') >= 0;
    }

    /**
     * Returns {@code true} if, after stripping all MiniMessage tags, the remaining text is blank
     * Used by ColorChatModule to cancel empty messages such as
     * a player sending only {@code "<green>"} or {@code "&a"}
     */
    public static boolean isBlankAfterTags(@NonNull String sanitized) {
        if (sanitized.isBlank()) return true;
        if (sanitized.indexOf('<') < 0) return false;
        return TAG_STRIP_PATTERN.matcher(sanitized).replaceAll("").isBlank();
    }

    private static @NonNull String processLegacy(@NonNull Player player, @NonNull String input) {
        if (input.indexOf('&') < 0) return input;

        Matcher m = LEGACY_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder(input.length());

        while (m.find()) {
            String code = m.group(1).toLowerCase();

            String replacement = ColorPermission.byLegacyCode(code)
                    .filter(cp -> player.hasPermission(cp.getNode()))
                    .map(ColorPermission::getPrimaryTagWrapped)
                    .orElse("");

            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }

        m.appendTail(sb);
        return sb.toString();
    }

    private static @NonNull String processMiniTags(@NonNull Player player, @NonNull String input) {
        if (input.indexOf('<') < 0) return input;

        Matcher m = MINI_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder(input.length());

        while (m.find()) {
            String tag = m.group(2).toLowerCase();
            boolean keep;

            if (tag.startsWith("#")) {
                keep = player.hasPermission(ColorPermission.HEX_COLOR.getNode());
            } else {
                keep = !ColorPermission.BLOCKED_TAGS.contains(tag)
                        && ColorPermission.byMiniTag(tag)
                        .map(cp -> player.hasPermission(cp.getNode()))
                        .orElse(false);
            }

            m.appendReplacement(sb, keep ? Matcher.quoteReplacement(m.group()) : "");
        }

        m.appendTail(sb);
        return sb.toString();
    }
}