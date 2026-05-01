package tect.host.tpl.module.impl.chat.group;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.config.ConfigFile;

import java.util.List;

public final class GroupService {

    private final ConfigFile groupsFile;
    private List<GroupEntry> groups = List.of();

    public GroupService(@NonNull ConfigFile groupsFile) {
        this.groupsFile = groupsFile;
        this.groupsFile.register();
    }

    public void reload() {
        groupsFile.reload();
        this.groups = new GroupModuleConfig(groupsFile.get()).getGroups();
    }

    // Groups are pre-sorted by priority (ascending) in GroupModuleConfig
    // Returns the first group whose permission the player has, or null if none match
    public @Nullable GroupEntry getPlayerGroup(@NonNull Player player) {
        for (GroupEntry group : groups) {
            if (!group.permission().isBlank() && player.hasPermission(group.permission())) {
                return group;
            }
        }
        return null;
    }

    public @NonNull String getPlayerGroupName(@NonNull Player player) {
        GroupEntry entry = getPlayerGroup(player);
        return entry != null ? entry.id() : "";
    }

    public @NonNull String getPlayerPrefix(@NonNull Player player) {
        GroupEntry entry = getPlayerGroup(player);
        return entry != null ? entry.prefix() : "";
    }

    public @NonNull String getPlayerSuffix(@NonNull Player player) {
        GroupEntry entry = getPlayerGroup(player);
        return entry != null ? entry.suffix() : "";
    }
}