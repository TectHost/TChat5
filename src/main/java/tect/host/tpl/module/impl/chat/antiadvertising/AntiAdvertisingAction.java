package tect.host.tpl.module.impl.chat.antiadvertising;

import org.jspecify.annotations.NonNull;

public enum AntiAdvertisingAction {
    BLOCK, CENSOR;

    public static @NonNull AntiAdvertisingAction fromString(@NonNull String value) {
        try {
            return AntiAdvertisingAction.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BLOCK;
        }
    }
}
