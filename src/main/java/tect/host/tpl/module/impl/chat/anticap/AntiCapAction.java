package tect.host.tpl.module.impl.chat.anticap;

import org.jspecify.annotations.NonNull;

import java.util.Locale;

public enum AntiCapAction {
    TO_LOWER_CASE,
    BLOCK,
    CENSOR;

    public static @NonNull AntiCapAction fromString(@NonNull String s) {
        return switch (s.trim().toUpperCase(Locale.ROOT)) {
            case "BLOCK" -> BLOCK;
            case "CENSOR" -> CENSOR;
            default -> TO_LOWER_CASE;
        };
    }
}