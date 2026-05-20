package tect.host.tpl.module.impl.chat.anticap;

import org.jspecify.annotations.NonNull;

public enum Action {
    TO_LOWER_CASE,
    BLOCK,
    CENSOR;

    public static @NonNull Action fromString(@NonNull String s) {
        return switch (s.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "BLOCK" -> BLOCK;
            case "CENSOR" -> CENSOR;
            default -> TO_LOWER_CASE;
        };
    }
}