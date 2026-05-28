package tect.host.tpl.module.impl.broadcast.autobroadcast;

import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AutoBroadcastConfig {

    private final long intervalSeconds;
    private final List<AutoBroadcastEntry> entries;

    public AutoBroadcastConfig(@NonNull ConfigFile configFile) {
        var cfg = configFile.get();

        this.intervalSeconds = Math.max(1L, cfg.getLong("options.time", 300L));

        List<AutoBroadcastEntry> loaded = new ArrayList<>();
        ConfigurationSection broadcasts = cfg.getConfigurationSection("broadcasts");
        if (broadcasts != null) {
            for (String id : broadcasts.getKeys(false)) {
                ConfigurationSection section = broadcasts.getConfigurationSection(id);
                if (section == null) continue;

                List<String> messages = section.getStringList("message").stream()
                        .filter(l -> l != null && !l.isBlank())
                        .toList();

                if (messages.isEmpty()) continue;

                String rawChannel = section.getString("channel", "");
                Optional<String> channel = rawChannel.isBlank() || rawChannel.equalsIgnoreCase("none") ? Optional.empty() : Optional.of(rawChannel);

                String rawPerm = section.getString("permission", "none");
                Optional<String> permission = rawPerm.isBlank() || rawPerm.equalsIgnoreCase("none") ? Optional.empty() : Optional.of(rawPerm);

                loaded.add(new AutoBroadcastEntry(id, messages, channel, permission));
            }
        }

        this.entries = List.copyOf(loaded);
    }

    public long getIntervalSeconds() { return intervalSeconds; }
    public @NonNull List<AutoBroadcastEntry> getEntries() { return entries; }
}