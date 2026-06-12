package tect.host.tpl.module.impl.chat.bridge;

import org.jspecify.annotations.NonNull;

import java.util.Set;

public record BridgeEntry(@NonNull String id, @NonNull Set<String> worlds) {

    public boolean contains(@NonNull String worldName) {
        return worlds.contains(worldName);
    }
}