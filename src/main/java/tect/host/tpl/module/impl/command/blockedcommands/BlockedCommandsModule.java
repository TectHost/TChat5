package tect.host.tpl.module.impl.command.blockedcommands;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigFile;
import tect.host.tpl.module.type.CommandModule;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.util.ColorUtil;
import tect.host.tpl.context.CommandContext;
import tect.host.tpl.util.Utils;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class BlockedCommandsModule implements CommandModule {

    private static final String ID = "blocked-commands";
    private static final String BYPASS_PERM = "tchat.admin.bypass.blockedcommands";

    private record LoadedState(@NonNull Set<String> blockedRoots, @NonNull List<Component> messageComponents) {
        @Contract("_ -> new")
        static @NonNull LoadedState from(@NonNull BlockedCommandsConfig config) {
            List<Component> components = config.getBlockMessage().stream()
                    .map(ColorUtil::legacyToMini)
                    .map(ColorUtil::deserialize)
                    .toList();
            return new LoadedState(config.getBlockedRoots(), components);
        }
    }

    private final ModuleContext moduleContext;
    private ConfigFile configFile;

    private volatile LoadedState state;

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
        state = LoadedState.from(new BlockedCommandsConfig(configFile));
    }

    @Override
    public void process(@NonNull CommandContext ctx) {
        LoadedState snap = state;
        if (snap == null || snap.blockedRoots().isEmpty()) return;
        if (Utils.hasPerms(ctx.getPlayer(), BYPASS_PERM)) return;

        String root = commandRoot(ctx.getEffectiveCommand());
        if (!snap.blockedRoots().contains(root)) return;

        ctx.cancel();

        List<Component> components = snap.messageComponents();
        if (!components.isEmpty()) {
            for (Component component : components) {
                ctx.getPlayer().sendMessage(component);
            }
        }
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