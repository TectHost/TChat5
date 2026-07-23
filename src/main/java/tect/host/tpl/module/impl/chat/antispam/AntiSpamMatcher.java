package tect.host.tpl.module.impl.chat.antispam;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.util.text.CensorUtil;
import tect.host.tpl.util.text.NormalizedText;
import tect.host.tpl.util.text.ObfuscationNormalizer;

import java.util.*;
import java.util.regex.Matcher;

public final class AntiSpamMatcher {

    private AntiSpamMatcher() {}

    public static @NonNull AntiSpamResult check(@NonNull String raw, @NonNull AntiSpamConfig config) {
        return checkOn(ObfuscationNormalizer.stripZeroWidth(raw).text(), config);
    }

    public static @Nullable String censor(@NonNull String raw, @NonNull AntiSpamConfig config, char censorChar) {
        NormalizedText normalized = ObfuscationNormalizer.stripZeroWidth(raw);
        String n = normalized.text();
        if (!checkOn(n, config).matched()) return null;

        boolean[] mask = new boolean[raw.length()];

        if (config.isCheckRepeatedChars()) {
            markMatches(normalized, config.getRepeatedCharsPattern().matcher(n), mask);
        }

        if (config.isCheckRepeatedPattern()) {
            markMatches(normalized, config.getRepeatedPatternPattern().matcher(n), mask);
        }

        if (config.isCheckRepeatedWords()) {
            Matcher m = config.getRepeatedWordsPattern().matcher(n);
            while (m.find()) {
                if (isWhitelisted(m.group(1), config.getWhitelistedWords())) continue;
                normalized.markRaw(mask, m.start(), m.end());
            }
        }

        if (config.isCheckCharDiversity() && isLowDiversity(n, config)) {
            normalized.markRaw(mask, 0, n.length());
        }

        String result = CensorUtil.applyMask(raw, mask, censorChar);
        return result != null ? result : CensorUtil.censorFull(raw, censorChar);
    }

    private static @NonNull AntiSpamResult checkOn(@NonNull String n, @NonNull AntiSpamConfig config) {
        if (config.isCheckRepeatedChars() && config.getRepeatedCharsPattern().matcher(n).find()) {
            return new AntiSpamResult(true, "repeated-chars");
        }

        if (config.isCheckRepeatedPattern() && config.getRepeatedPatternPattern().matcher(n).find()) {
            return new AntiSpamResult(true, "repeated-pattern");
        }

        if (config.isCheckRepeatedWords()) {
            Matcher m = config.getRepeatedWordsPattern().matcher(n);
            while (m.find()) {
                if (isWhitelisted(m.group(1), config.getWhitelistedWords())) continue;
                return new AntiSpamResult(true, "repeated-words");
            }
        }

        if (config.isCheckCharDiversity() && isLowDiversity(n, config)) {
            return new AntiSpamResult(true, "char-diversity");
        }

        return AntiSpamResult.CLEAN;
    }

    private static void markMatches(@NonNull NormalizedText normalized, @NonNull Matcher m, boolean @NonNull [] mask) {
        while (m.find()) normalized.markRaw(mask, m.start(), m.end());
    }

    private static boolean isLowDiversity(@NonNull String n, @NonNull AntiSpamConfig config) {
        String stripped = n.replace(" ", "");
        int len = stripped.length();
        if (len < config.getCharDiversityMinLength()) return false;

        char[] chars = new char[len];
        for (int i = 0; i < len; i++) chars[i] = Character.toLowerCase(stripped.charAt(i));
        Arrays.sort(chars);

        int unique = 1;
        for (int i = 1; i < len; i++) {
            if (chars[i] != chars[i - 1]) unique++;
        }

        double ratio = (double) unique / len;
        return ratio < config.getCharDiversityMinRatio();
    }

    private static boolean isWhitelisted(@NonNull String word, @NonNull List<String> whitelist) {
        String lower = word.toLowerCase(Locale.ROOT);
        for (String w : whitelist) {
            if (lower.equals(w)) return true;
        }
        return false;
    }
}