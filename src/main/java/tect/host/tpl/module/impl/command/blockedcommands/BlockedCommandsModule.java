package tect.host.tpl.module.impl.command.blockedcommands;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.config.ConfigFile;
import tect.host.tpl.module.type.CommandModule;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.context.CommandContext;
import tect.host.tpl.util.Utils;

import java.util.Locale;

public final class BlockedCommandsModule implements CommandModule {

    private static final String ID = "blocked-commands";
    private static final String BYPASS_PERM = "tchat.admin.bypass.blockedcommands";

    private final ModuleContext moduleContext;
    private ConfigFile configFile;

    private volatile @Nullable BlockedCommandsConfig config;

    public BlockedCommandsModule(@NonNull ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    @Override
    public void onEnable() {
        configFile = moduleContext.createConfigFile("blockedcommands.yml", "modules");
        configFile.setMigrator(BlockedCommandsMigrations.create(moduleContext.getLogger()));
        configFile.register();
        load();
    }

    @Override
    public void onReload() {
        configFile.reload();
        load();
    }

    private void load() {
        config = new BlockedCommandsConfig(configFile);
    }

    @Override
    public void process(@NonNull CommandContext ctx) {
        BlockedCommandsConfig cfg = config;
        if (cfg == null || cfg.getBlockedRoots().isEmpty()) return;
        if (Utils.hasPerms(ctx.getPlayer(), BYPASS_PERM)) return;

        String root = commandRoot(ctx.getEffectiveCommand());
        if (!cfg.getBlockedRoots().contains(root)) return;

        ctx.cancel();

        moduleContext.getActionExecutor().execute(ctx.getPlayer(), cfg.getActions());
    }

    /**
     * Extracts the lowercased root from a raw command string
     * "/me some text" -> "me"
     */
    private static @NonNull String commandRoot(@NonNull String rawCommand) {
        String withoutSlash = rawCommand.substring(1);
        int space = withoutSlash.indexOf(' ');
        String root = space == -1 ? withoutSlash : withoutSlash.substring(0, space);
        return root.toLowerCase(Locale.ROOT);
    }

    @Override
    public @NonNull String getId() { return ID; }
}