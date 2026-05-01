package tect.host.tpl.module.impl.chat.group;

import org.jspecify.annotations.NonNull;

public record GroupEntry(
        @NonNull String id,
        @NonNull String permission,
        int priority,
        @NonNull String prefix,
        @NonNull String suffix,
        @NonNull String format
) {
}
