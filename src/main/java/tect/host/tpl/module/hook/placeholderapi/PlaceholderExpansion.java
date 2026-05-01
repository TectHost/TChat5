package tect.host.tpl.module.hook.placeholderapi;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.TChat;
import tect.host.tpl.manager.ModuleManager;
import tect.host.tpl.module.impl.chat.group.GroupModule;
import tect.host.tpl.module.impl.chat.group.GroupService;
import tect.host.tpl.util.ColorUtil;

public final class PlaceholderExpansion extends me.clip.placeholderapi.expansion.PlaceholderExpansion {

    private final TChat plugin;
    private final ModuleManager moduleManager;

    private @Nullable GroupService groupService;

    public PlaceholderExpansion(@NonNull TChat plugin, @NonNull ModuleManager moduleManager) {
        this.plugin = plugin;
        this.moduleManager = moduleManager;
    }

    public void refreshGroupService() {
        GroupModule groupModule = moduleManager.getModule("group", GroupModule.class);
        this.groupService = groupModule != null ? groupModule.getGroupService() : null;
    }

    @Override public @NonNull String getIdentifier() { return "tchat"; }
    @Override public @NonNull String getAuthor() { return String.join(", ", plugin.getPluginMeta().getAuthors()); }
    @Override public @NonNull String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NonNull String identifier) {
        if (player == null) return "";

        return switch (identifier) {
            case "prefix" -> groupService != null ? ColorUtil.legacyToMini(groupService.getPlayerPrefix(player)) : "";
            case "suffix" -> groupService != null ? ColorUtil.legacyToMini(groupService.getPlayerSuffix(player)) : "";
            case "group"  -> groupService != null ? groupService.getPlayerGroupName(player) : "";
            case "nick" -> ColorUtil.toPlainText(player.displayName()); // In the future, this will call the nicknames module
            default -> null;
        };
    }
}