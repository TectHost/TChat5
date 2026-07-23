package tect.host.tpl.util.text;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.regex.Matcher;

public final class MaskUtil {

    private MaskUtil() {}

    public static void applyMatcher(@NonNull Matcher matcher, boolean @NonNull [] mask) {
        while (matcher.find()) {
            Arrays.fill(mask, matcher.start(), matcher.end(), true);
        }
    }

    public static void applyRange(boolean @NonNull [] mask, int from, int to) {
        Arrays.fill(mask, from, to, true);
    }
}