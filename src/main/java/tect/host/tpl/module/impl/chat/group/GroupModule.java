package tect.host.tpl.module.impl.chat.group;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.config.ConfigFile;
import tect.host.tpl.module.type.ChatModule;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.pipeline.ChatRenderer;
import tect.host.tpl.context.MessageContext;

public class GroupModule implements ChatModule {

    private final ModuleContext moduleContext;
    private GroupService groupService;

    public GroupModule(@NonNull ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    @Override
    public void onEnable() {
        ConfigFile groupsFile = moduleContext.createConfigFile("groups.yml", "modules");
        groupsFile.setMigrator(GroupMigrations.create(moduleContext.getLogger()));
        this.groupService = new GroupService(groupsFile);
        groupService.reload();
    }

    @Override
    public void onReload() {
        groupService.reload();
    }

    @Override
    public void onDisable() {
        groupService = null;
    }

    @Override
    public void process(@NonNull MessageContext ctx) {
        if (ctx.getFormat() != null) return;

        Player player = ctx.getPlayer();
        GroupEntry group = groupService.getPlayerGroup(player);
        if (group == null || group.format().isBlank()) return;

        ctx.setFormat(ChatRenderer.render(
                moduleContext.getPlaceholderApiHook(), player, group.format(), ctx
        ));
    }

    /**
     * Provides access to the GroupService owned by this module
     * Only returns null after onDisable(), not during normal operation
     */
    public @Nullable GroupService getGroupService() {
        return groupService;
    }

    @Override
    public @NonNull String getId() { return "group"; }
}