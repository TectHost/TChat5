package tect.host.tpl.module.impl.chat.grammar;

import org.jspecify.annotations.NonNull;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GrammarProcessor {

    private GrammarProcessor() {}

    private static final Pattern MULTI_SPACE = Pattern.compile(" {2,}");
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?]\\s)(\\p{L})");

    public static @NonNull String collapseSpaces(@NonNull String raw) {
        return MULTI_SPACE.matcher(raw.strip()).replaceAll(" ");
    }

    public static @NonNull String applyCap(@NonNull String raw, int letters) {
        int end = Math.min(letters, raw.length());
        return raw.substring(0, end).toUpperCase() + raw.substring(end);
    }

    public static @NonNull String applySentenceCase(@NonNull String raw) {
        Matcher m = SENTENCE_BOUNDARY.matcher(raw);
        if (!m.find()) return raw;

        StringBuilder sb = new StringBuilder(raw);
        do {
            sb.setCharAt(m.start(), Character.toUpperCase(sb.charAt(m.start())));
        } while (m.find());

        return sb.toString();
    }

    public static @NonNull String applyFinalDot(@NonNull String raw, @NonNull String dotCharacter, @NonNull Set<Character> ignoredEndings) {
        if (raw.isEmpty()) return raw;

        char last = raw.charAt(raw.length() - 1);
        if (ignoredEndings.contains(last)) return raw;

        return raw + dotCharacter;
    }
}