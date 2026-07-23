package tect.host.tpl.module.impl.chat.antiunicode;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.util.text.CensorUtil;
import tect.host.tpl.util.text.MaskUtil;

import java.util.regex.Pattern;

public final class AntiUnicodeMatcher {

    private AntiUnicodeMatcher() {}

    // Bidirectional control characters (RLO/LRO/PDF/isolates/ALM) (CVE-2021-42574)
    private static final Pattern BIDI_CONTROL = Pattern.compile("[\\x{202A}-\\x{202E}\\x{2066}-\\x{2069}\\x{200E}\\x{200F}\\x{061C}]");

    // Zero-width / invisible formatting characters and variation selectors
    private static final Pattern ZERO_WIDTH = Pattern.compile("[\\x{200B}-\\x{200D}\\x{FEFF}\\x{00AD}\\x{2060}-\\x{2064}\\x{180E}\\x{FE00}-\\x{FE0F}\\x{E0100}-\\x{E01EF}]");

    // Non-printable ASCII control characters
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x{0000}-\\x{0008}\\x{000B}\\x{000C}\\x{000E}-\\x{001F}\\x{007F}]");

    // Fullwidth Forms + Mathematical Alphanumeric Symbols
    private static final Pattern FULLWIDTH = Pattern.compile("[\\x{FF00}-\\x{FFEF}\\x{1D400}-\\x{1D7FF}]");

    // Private Use Area characters (BMP + supplementary planes A/B), custom icon/font glyph abuse
    private static final Pattern PRIVATE_USE_AREA = Pattern.compile("[\\x{E000}-\\x{F8FF}\\x{F0000}-\\x{FFFFD}\\x{100000}-\\x{10FFFD}]");

    // Common emoji ranges (pictographs, symbols, dingbats, regional indicators)
    private static final Pattern EMOJI = Pattern.compile("[\\x{1F300}-\\x{1FAFF}\\x{2600}-\\x{27BF}\\x{2B00}-\\x{2BFF}\\x{1F1E6}-\\x{1F1FF}\\x{2190}-\\x{21FF}\\x{2300}-\\x{23FF}]");

    public static @NonNull AntiUnicodeResult check(@NonNull String raw, @NonNull AntiUnicodeConfig config) {
        if (config.isCheckBidiControl() && BIDI_CONTROL.matcher(raw).find())
            return new AntiUnicodeResult(true, "bidi-control");

        if (config.isCheckZeroWidth() && ZERO_WIDTH.matcher(raw).find())
            return new AntiUnicodeResult(true, "zero-width");

        if (config.isCheckControlChars() && CONTROL_CHARS.matcher(raw).find())
            return new AntiUnicodeResult(true, "control-chars");

        if (config.isCheckFullwidth() && FULLWIDTH.matcher(raw).find())
            return new AntiUnicodeResult(true, "fullwidth");

        if (config.isCheckPrivateUseArea() && PRIVATE_USE_AREA.matcher(raw).find())
            return new AntiUnicodeResult(true, "private-use-area");

        if (config.isCheckEmoji() && EMOJI.matcher(raw).find())
            return new AntiUnicodeResult(true, "emoji");

        if (config.isCheckZalgo() && hasExcessiveCombiningMarks(raw, config.getMaxCombiningMarks()))
            return new AntiUnicodeResult(true, "zalgo");

        if (config.isCheckScripts() && hasDisallowedScript(raw, config))
            return new AntiUnicodeResult(true, "script");

        return AntiUnicodeResult.CLEAN;
    }

    /** Removes only the offending characters, keeping the rest of the message intact. */
    public static @NonNull String strip(@NonNull String raw, @NonNull AntiUnicodeConfig config) {
        boolean[] mask = buildMask(raw, config);

        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            if (!mask[i]) sb.append(raw.charAt(i));
        }
        return sb.toString();
    }

    /** Replaces only the offending characters with censorChar. Returns null if nothing matched. */
    public static @Nullable String censor(@NonNull String raw, @NonNull AntiUnicodeConfig config, char censorChar) {
        boolean[] mask = buildMask(raw, config);
        return CensorUtil.applyMask(raw, mask, censorChar);
    }

    private static boolean @NonNull [] buildMask(@NonNull String raw, @NonNull AntiUnicodeConfig config) {
        boolean[] mask = new boolean[raw.length()];

        if (config.isCheckBidiControl()) MaskUtil.applyMatcher(BIDI_CONTROL.matcher(raw), mask);
        if (config.isCheckZeroWidth()) MaskUtil.applyMatcher(ZERO_WIDTH.matcher(raw), mask);
        if (config.isCheckControlChars()) MaskUtil.applyMatcher(CONTROL_CHARS.matcher(raw), mask);
        if (config.isCheckFullwidth()) MaskUtil.applyMatcher(FULLWIDTH.matcher(raw), mask);
        if (config.isCheckPrivateUseArea()) MaskUtil.applyMatcher(PRIVATE_USE_AREA.matcher(raw), mask);
        if (config.isCheckEmoji()) MaskUtil.applyMatcher(EMOJI.matcher(raw), mask);
        if (config.isCheckZalgo()) maskExcessiveCombiningMarks(raw, mask, config.getMaxCombiningMarks());
        if (config.isCheckScripts()) maskDisallowedScript(raw, mask, config);

        return mask;
    }

    private static boolean hasExcessiveCombiningMarks(@NonNull String raw, int max) {
        int run = 0;
        for (int i = 0; i < raw.length(); ) {
            int cp = raw.codePointAt(i);
            if (isCombiningMark(cp)) {
                run++;
                if (run > max) return true;
            } else {
                run = 0;
            }
            i += Character.charCount(cp);
        }
        return false;
    }

    private static void maskExcessiveCombiningMarks(@NonNull String raw, boolean[] mask, int max) {
        int run = 0;
        int runStart = -1;

        for (int i = 0; i < raw.length(); ) {
            int cp = raw.codePointAt(i);
            int charLen = Character.charCount(cp);

            if (isCombiningMark(cp)) {
                if (run == 0) runStart = i;
                run++;
                if (run > max) MaskUtil.applyRange(mask, runStart, i + charLen);
            } else {
                run = 0;
                runStart = -1;
            }

            i += charLen;
        }
    }

    private static boolean isCombiningMark(int cp) {
        int type = Character.getType(cp);
        return type == Character.NON_SPACING_MARK || type == Character.ENCLOSING_MARK || type == Character.COMBINING_SPACING_MARK;
    }

    private static boolean hasDisallowedScript(@NonNull String raw, @NonNull AntiUnicodeConfig config) {
        for (int i = 0; i < raw.length(); ) {
            int cp = raw.codePointAt(i);
            if (isDisallowed(cp, config)) return true;
            i += Character.charCount(cp);
        }
        return false;
    }

    private static void maskDisallowedScript(@NonNull String raw, boolean[] mask, @NonNull AntiUnicodeConfig config) {
        for (int i = 0; i < raw.length(); ) {
            int cp = raw.codePointAt(i);
            int charLen = Character.charCount(cp);
            if (isDisallowed(cp, config)) MaskUtil.applyRange(mask, i, i + charLen);
            i += charLen;
        }
    }

    private static boolean isDisallowed(int cp, @NonNull AntiUnicodeConfig config) {
        if (!Character.isLetter(cp)) return false;
        Character.UnicodeScript script;
        try {
            script = Character.UnicodeScript.of(cp);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return !config.getAllowedScripts().contains(script);
    }
}