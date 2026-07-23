package tect.host.tpl.module.impl.chat.blockedwords;

import org.jspecify.annotations.NonNull;

public enum BlockedWordsAction {
    BLOCK,
    CENSOR,
    CENSOR_ALL;

    public static @NonNull BlockedWordsAction fromString(@NonNull String s) {
        return switch (s.toUpperCase()) {
            case "BLOCK" -> BLOCK;
            case "CENSOR_ALL" -> CENSOR_ALL;
            default -> CENSOR;
        };
    }
}