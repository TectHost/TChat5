package tect.host.tpl.module.impl.command.commandcooldown;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.action.CompiledActions;
import tect.host.tpl.config.ConfigFile;
import tect.host.tpl.context.CommandContext;
import tect.host.tpl.context.QuitContext;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.type.CommandModule;
import tect.host.tpl.module.type.QuitModule;
import tect.host.tpl.util.Utils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CommandCooldownModule implements CommandModule, QuitModule {

    private static final String ID = "command-cooldown";

    private static final String BYPASS_PERM = "tchat.admin.bypass.commandcooldown";
    private static final String PLACEHOLDER_REMAINING = "%cooldown_remaining%";

    private final ModuleContext moduleContext;
    private ConfigFile configFile;

    private volatile @Nullable CommandCooldownConfig config;
    private volatile CompiledActions compiledActions = CompiledActions.EMPTY;

    private final Map<UUID, Long> lastCommandAt = new ConcurrentHashMap<>();

    public CommandCooldownModule(@NonNull ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    @Override
    public void onEnable() {
        configFile = moduleContext.createConfigFile("commandcooldown.yml", "modules");
        configFile.setMigrator(CommandCooldownMigrations.create(moduleContext.getLogger()));
        configFile.register();
        load();
    }

    @Override
    public void onReload() {
        configFile.reload();
        load();
    }

    private void load() {
        CommandCooldownConfig cfg = new CommandCooldownConfig(configFile);
        config = cfg;
        compiledActions = moduleContext.getActionExecutor().compile(cfg.getActions());
    }

    @Override
    public void process(@NonNull CommandContext ctx) {
        CommandCooldownConfig cfg = config;
        if (cfg == null || cfg.getCooldownMillis() <= 0) return;

        Player player = ctx.getPlayer();
        if (Utils.hasPerms(player, BYPASS_PERM)) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastCommandAt.get(uuid);

        if (last != null) {
            long elapsed = now - last;
            if (elapsed < cfg.getCooldownMillis()) {
                long remainingSeconds = Math.floorDiv(cfg.getCooldownMillis() - elapsed + 999, 1000L);
                moduleContext.getActionExecutor().execute(player, compiledActions, Map.of(PLACEHOLDER_REMAINING, String.valueOf(remainingSeconds)));
                ctx.cancel();
                return;
            }
        }

        ctx.addOnSuccessHook(() -> lastCommandAt.put(uuid, now));
    }

    @Override
    public void onQuit(@NonNull QuitContext quitCtx) {
        lastCommandAt.remove(quitCtx.getPlayer().getUniqueId());
    }

    @Override
    public @NonNull String getId() { return ID; }
}