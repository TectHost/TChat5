package tect.host.tpl.module.impl.broadcast.autobroadcast;

import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;

public record AutoBroadcastEntry(@NonNull String id, @NonNull List<String> rawMessages, @NonNull Optional<String> channel, @NonNull Optional<String> permission) {
    public AutoBroadcastEntry {
        rawMessages = List.copyOf(rawMessages);
    }
}