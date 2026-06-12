package tect.host.tpl.module.impl.chat.worlds;

import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigFile;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class WorldsConfig {

    private final Map<String, WorldConfig> worlds;

    public WorldsConfig(@NonNull ConfigFile configFile) {
        ConfigurationSection root = configFile.get().getConfigurationSection("worlds");
        if (root == null) {
            this.worlds = Map.of();
            return;
        }

        Set<String> keys = root.getKeys(false);
        Map<String, WorldConfig> map = new LinkedHashMap<>(keys.size());
        for (String worldName : keys) {
            ConfigurationSection sec = root.getConfigurationSection(worldName);
            if (sec == null) continue;
            map.put(worldName, WorldConfig.parse(sec));
        }
        this.worlds = Collections.unmodifiableMap(map);
    }

    public @NonNull WorldConfig forWorld(@NonNull String worldName) {
        return worlds.getOrDefault(worldName, WorldConfig.defaultConfig());
    }
}