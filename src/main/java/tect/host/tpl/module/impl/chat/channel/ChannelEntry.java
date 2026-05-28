package tect.host.tpl.module.impl.chat.channel;

import org.jspecify.annotations.NonNull;

public record ChannelEntry(@NonNull String id, @NonNull String permission, @NonNull String format, int messageMode, int announceMode, int limit) {
    public boolean hasLimit() {
        return limit > 0;
    }
}