package tect.host.tpl.module.impl.chat.antiunicode;

import org.jspecify.annotations.NonNull;

public enum AntiUnicodeAction {
    BLOCK,
    CENSOR,
    STRIP;

    public static @NonNull AntiUnicodeAction fromString(@NonNull String value) {
        try {
            return AntiUnicodeAction.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BLOCK;
        }
    }
}