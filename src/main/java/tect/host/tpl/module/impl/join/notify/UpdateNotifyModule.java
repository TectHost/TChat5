package tect.host.tpl.module.impl.join.notify;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.module.type.JoinModule;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.context.JoinContext;
import tect.host.tpl.util.Utils;

import java.util.Map;

public final class UpdateNotifyModule implements JoinModule {

    private static final String PERMISSION = "tchat.admin.check-updates";

    private final ModuleContext moduleContext;

    public UpdateNotifyModule(@NonNull ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    private void checkForUpdates() {
        UpdateChecker.check(
                moduleContext.getScheduler(),
                moduleContext.getLogger(),
                moduleContext.getPluginVersion()
        );
    }

    @Override
    public void onEnable() {
        checkForUpdates();
    }

    @Override
    public void onReload() {
        checkForUpdates();
    }

    @Override
    public void onJoin(@NonNull JoinContext ctx) {
        Player player = ctx.getPlayer();
        if (!Utils.hasPerms(player, PERMISSION)) return;

        Boolean outdated = UpdateChecker.isOutdated();
        if (outdated == null || !outdated) return;

        moduleContext.getMessagesManager().sendMessage(player, "update-notify-outdated", Map.of(
                "%latest%", String.valueOf(UpdateChecker.getLatestVersion()),
                "%current%", String.valueOf(UpdateChecker.getCurrentVersion())
        ));
    }

    @Override
    public @NonNull String getId() { return "update-notify"; }
}