package tect.host.tpl.util.text;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class CensorUtil {

    private CensorUtil() {}

    public static @Nullable String applyMask(@NonNull String original, boolean @NonNull [] mask, char censorChar) {
        boolean any = false;
        for (boolean b : mask) { if (b) { any = true; break; } }
        if (!any) return null;

        char[] result = original.toCharArray();
        for (int i = 0; i < result.length; i++) {
            if (mask[i]) result[i] = censorChar;
        }
        return new String(result);
    }

    public static @NonNull String censorFull(@NonNull String original, char censorChar) {
        char[] full = new char[original.length()];
        java.util.Arrays.fill(full, censorChar);
        return new String(full);
    }
}