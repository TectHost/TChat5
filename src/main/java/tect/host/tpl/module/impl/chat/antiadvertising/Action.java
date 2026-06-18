package tect.host.tpl.module.impl.chat.antiadvertising;

import org.jspecify.annotations.NonNull;

public enum Action {
    BLOCK, CENSOR;

    public static @NonNull Action fromString(@NonNull String value) {
        try {
            return Action.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BLOCK;
        }
    }
}
