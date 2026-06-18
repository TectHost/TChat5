package tect.host.tpl.module.impl.chat.antiadvertising;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.config.ConfigFile;
import tect.host.tpl.context.MessageContext;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.type.ChatModule;
import tect.host.tpl.util.Utils;

public final class AntiAdvertisingModule implements ChatModule {

    private static final String ID = "anti-advertising";

    private static final String BYPASS_PERM = "tchat.admin.bypass.antiadvertising";

    private final ModuleContext moduleContext;
    private ConfigFile configFile;

    private volatile @Nullable AntiAdvertisingConfig config;

    public AntiAdvertisingModule(@NonNull ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    @Override
    public void onEnable() {
        configFile = moduleContext.createConfigFile("antiadvertising.yml", "modules");
        configFile.setMigrator(AntiAdvertisingMigrations.create(moduleContext.getLogger()));
        configFile.register();
        load();
    }

    @Override
    public void onReload() {
        configFile.reload();
        load();
    }

    private void load() {
        config = new AntiAdvertisingConfig(configFile, moduleContext.getLogger());
    }

    @Override
    public void process(@NonNull MessageContext ctx) {
        AntiAdvertisingConfig cfg = config;
        if (cfg == null) return;

        Player player = ctx.getPlayer();
        if (Utils.hasPerms(player, BYPASS_PERM)) return;

        String raw = ctx.getEffectiveRaw();

        switch (cfg.getAction()) {
            case BLOCK -> {
                if (!AntiAdvertisingMatcher.check(raw, cfg).matched()) return;
                moduleContext.getActionExecutor().execute(player, cfg.getActions());
                ctx.setCancelled(true);
            }
            case CENSOR -> {
                String censored = AntiAdvertisingMatcher.censor(raw, cfg, cfg.getCensorChar());
                if (censored == null) return;
                moduleContext.getActionExecutor().execute(player, cfg.getActions());
                ctx.setRawOverride(censored);
            }
            default -> Utils.log(moduleContext.getLogger(), "WARNING", ("Invalid action ('%s') in Anti Advertising module").formatted(cfg.getAction()));
        }
    }

    @Override
    public @NonNull String getId() { return ID; }
}