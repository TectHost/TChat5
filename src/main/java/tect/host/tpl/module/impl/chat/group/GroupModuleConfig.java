package tect.host.tpl.module.impl.chat.group;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class GroupModuleConfig {

    private final List<GroupEntry> groups;

    public GroupModuleConfig(@NonNull FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("groups");
        if (section == null) {
            this.groups = List.of();
            return;
        }

        List<GroupEntry> loadedGroups = new ArrayList<>();
        for (String groupId : section.getKeys(false)) {
            ConfigurationSection groupSection = section.getConfigurationSection(groupId);
            if (groupSection == null) continue;

            loadedGroups.add(new GroupEntry(
                    groupId,
                    groupSection.getString("permission", ""),
                    groupSection.getInt("priority", 1000),
                    groupSection.getString("prefix", ""),
                    groupSection.getString("suffix", ""),
                    groupSection.getString("format", "")
            ));
        }

        loadedGroups.sort(Comparator.comparingInt(GroupEntry::priority));
        this.groups = List.copyOf(loadedGroups);
    }

    public @NonNull List<GroupEntry> getGroups() {
        return groups;
    }
}