package tect.host.tpl.module.impl.chat.antiunicode;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.config.ConfigFile;
import tect.host.tpl.context.MessageContext;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.type.ChatModule;
import tect.host.tpl.util.Utils;

public final class AntiUnicodeModule implements ChatModule {

    private static final String ID = "anti-unicode";

    private static final String BYPASS_PERM = "tchat.admin.bypass.antiunicode";

    private final ModuleContext moduleContext;
    private ConfigFile configFile;

    private volatile @Nullable AntiUnicodeConfig config;

    public AntiUnicodeModule(@NonNull ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    @Override
    public void onEnable() {
        configFile = moduleContext.createConfigFile("antiunicode.yml", "modules");
        configFile.setMigrator(AntiUnicodeMigrations.create(moduleContext.getLogger()));
        configFile.register();
        load();
    }

    @Override
    public void onReload() {
        configFile.reload();
        load();
    }

    private void load() {
        config = new AntiUnicodeConfig(configFile, moduleContext.getLogger());
    }

    @Override
    public void process(@NonNull MessageContext ctx) {
        AntiUnicodeConfig cfg = config;
        if (cfg == null) return;

        Player player = ctx.getPlayer();
        if (Utils.hasPerms(player, BYPASS_PERM)) return;

        String raw = ctx.getEffectiveRaw();

        if (!AntiUnicodeMatcher.check(raw, cfg).matched()) return;

        switch (cfg.getAction()) {
            case BLOCK -> {
                moduleContext.getActionExecutor().execute(player, cfg.getActions());
                ctx.setCancelled(true);
            }
            case CENSOR -> {
                String censored = AntiUnicodeMatcher.censor(raw, cfg, cfg.getCensorChar());
                if (censored == null) return;
                moduleContext.getActionExecutor().execute(player, cfg.getActions());
                ctx.setRawOverride(censored);
            }
            case STRIP -> {
                String stripped = AntiUnicodeMatcher.strip(raw, cfg);
                if (stripped.equals(raw)) return;
                moduleContext.getActionExecutor().execute(player, cfg.getActions());
                ctx.setRawOverride(stripped);
            }
        }
    }

    @Override
    public @NonNull String getId() { return ID; }
}