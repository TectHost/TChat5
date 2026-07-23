package tect.host.tpl.util.text;

import org.jspecify.annotations.NonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizes obfuscated text and keeps track of the original raw range for
 * each emitted character, allowing matches to be mapped back to the source.
 */
public final class ObfuscationNormalizer {

    private ObfuscationNormalizer() {}

    private static final Pattern ZERO_WIDTH = Pattern.compile("[\\u200B-\\u200D\\uFEFF\\u00AD\\u2060\\u180E]");

    private static final Pattern DOT_WORD = Pattern.compile("\\s*(?:\\(dot\\)|\\[dot]|\\{dot}|(?<![a-z0-9])dot(?![a-z0-9]))\\s*", Pattern.CASE_INSENSITIVE);

    private static final Pattern COLON_WORD = Pattern.compile("\\s*(?:\\(colon\\)|\\[colon])\\s*", Pattern.CASE_INSENSITIVE);

    private static final Pattern SPACE_AROUND_PUNCT = Pattern.compile("\\s*([.:])\\s*");

    /**
     * It only removes zero-width characters, preserving the rest on a 1:1 basis.
     * Used by AntiSpam
     */
    public static @NonNull NormalizedText stripZeroWidth(@NonNull String raw) {
        NormalizedText.Builder b = NormalizedText.builder(raw);
        Matcher m = ZERO_WIDTH.matcher(raw);
        int last = 0;
        while (m.find()) {
            copyThrough(b, raw, last, m.start());
            last = m.end();
        }
        copyThrough(b, raw, last, raw.length());
        return b.build();
    }

    /**
     * Normalizes text for AntiAdvertising in one pass, handling zero-width
     * characters, obfuscated dots/colons, and surrounding whitespace
     */
    public static @NonNull NormalizedText normalizeObfuscation(@NonNull String raw) {
        NormalizedText.Builder b = NormalizedText.builder(raw);
        int n = raw.length();

        Matcher dotMatcher = DOT_WORD.matcher(raw);
        Matcher colonMatcher = COLON_WORD.matcher(raw);
        Matcher spaceMatcher = SPACE_AROUND_PUNCT.matcher(raw);

        int i = 0;
        while (i < n) {
            char c = raw.charAt(i);

            if (isZeroWidth(c)) {
                i++;
                continue;
            }

            dotMatcher.region(i, n).useTransparentBounds(true).useAnchoringBounds(false);
            if (dotMatcher.lookingAt()) {
                b.emit('.', i, dotMatcher.end());
                i = dotMatcher.end();
                continue;
            }

            colonMatcher.region(i, n).useTransparentBounds(true).useAnchoringBounds(false);
            if (colonMatcher.lookingAt()) {
                b.emit(':', i, colonMatcher.end());
                i = colonMatcher.end();
                continue;
            }

            if (c == '.' || c == ':' || Character.isWhitespace(c)) {
                spaceMatcher.region(i, n).useTransparentBounds(true).useAnchoringBounds(false);
                if (spaceMatcher.lookingAt() && spaceMatcher.group(1) != null) {
                    b.emit(spaceMatcher.group(1).charAt(0), i, spaceMatcher.end());
                    i = spaceMatcher.end();
                    continue;
                }
            }

            b.emit(c, i, i + 1);
            i++;
        }

        return b.build();
    }

    private static boolean isZeroWidth(char c) {
        return c == '\u200B' || c == '\u200C' || c == '\u200D' || c == '\uFEFF' || c == '\u00AD' || c == '\u2060' || c == '\u180E';
    }

    private static void copyThrough(NormalizedText.Builder b, @NonNull String raw, int from, int to) {
        for (int i = from; i < to; i++) b.emit(raw.charAt(i), i, i + 1);
    }
}