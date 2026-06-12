package tect.host.tpl.module.impl.chat.bridge;

import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.config.ConfigFile;

import java.util.*;

public final class ChatBridgeConfig {

    private final Map<String, BridgeEntry> worldIndex;

    public ChatBridgeConfig(@NonNull ConfigFile configFile) {
        ConfigurationSection root = configFile.get().getConfigurationSection("bridges");
        if (root == null) {
            this.worldIndex = Map.of();
            return;
        }

        Set<String> bridgeIds = root.getKeys(false);
        // assume ~3 worlds per bridge on average
        Map<String, BridgeEntry> index = new HashMap<>(bridgeIds.size() * 3);

        for (String bridgeId : bridgeIds) {
            List<String> worldList = root.getStringList(bridgeId);
            if (worldList.isEmpty()) continue;

            // Deduplicate and make immutable
            Set<String> worlds = Set.copyOf(worldList.stream()
                    .filter(w -> w != null && !w.isBlank())
                    .map(String::strip)
                    .toList());

            if (worlds.isEmpty()) continue;

            BridgeEntry entry = new BridgeEntry(bridgeId, worlds);
            for (String world : worlds) {
                index.put(world, entry);
            }
        }

        this.worldIndex = Collections.unmodifiableMap(index);
    }

    public @Nullable BridgeEntry bridgeForWorld(@NonNull String worldName) {
        return worldIndex.get(worldName);
    }
}