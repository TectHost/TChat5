package tect.host.tpl.module.impl.chat.antispam;

import org.jspecify.annotations.NonNull;

public enum AntiSpamAction {
    BLOCK,
    CENSOR;

    public static @NonNull AntiSpamAction fromString(@NonNull String value) {
        try {
            return AntiSpamAction.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BLOCK;
        }
    }
}