package tect.host.tpl.util;

import org.jspecify.annotations.NonNull;
import java.util.regex.Matcher;

public final class MaskUtil {

    private MaskUtil() {}

    public static void applyMatcher(@NonNull Matcher matcher, boolean @NonNull [] mask) {
        while (matcher.find()) {
            for (int i = matcher.start(); i < matcher.end(); i++) mask[i] = true;
        }
    }

    public static void applyRange(boolean @NonNull [] mask, int from, int to) {
        for (int i = from; i < to; i++) mask[i] = true;
    }
}