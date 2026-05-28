package tect.host.tpl.module.impl.chat.channel;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class ChannelModuleConfig {

    private final List<ChannelEntry> channels;

    public ChannelModuleConfig(@NonNull FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("channels");
        if (section == null) {
            this.channels = List.of();
            return;
        }

        List<ChannelEntry> loaded = new ArrayList<>();
        for (String channelId : section.getKeys(false)) {
            ConfigurationSection ch = section.getConfigurationSection(channelId);
            if (ch == null) continue;

            String format = ch.getString("format", "");
            String permission = ch.getString("permission", "tchat.channel." + channelId);
            int messageMode = ch.getInt("message-mode", 1);
            int announceMode = ch.getInt("announce-mode", 1);
            int limit = ch.getInt("limit", 0);

            loaded.add(new ChannelEntry(channelId, permission, format, messageMode, announceMode, limit));
        }

        this.channels = List.copyOf(loaded);
    }

    public @NonNull List<ChannelEntry> getChannels() {
        return channels;
    }
}