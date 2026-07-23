package tect.host.tpl.module.impl.chat.grammar;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.config.ConfigFile;
import tect.host.tpl.context.MessageContext;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.type.ChatModule;
import tect.host.tpl.util.Utils;

public final class GrammarModule implements ChatModule {

    private static final String ID = "grammar";

    private static final String BYPASS_PERM = "tchat.admin.bypass.grammar";

    private final ModuleContext moduleContext;
    private ConfigFile configFile;

    private volatile @Nullable GrammarConfig config;

    public GrammarModule(@NonNull ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    @Override
    public void onEnable() {
        configFile = moduleContext.createConfigFile("grammar.yml", "modules");
        configFile.setMigrator(GrammarMigrations.create(moduleContext.getLogger()));
        configFile.register();
        load();
    }

    @Override
    public void onReload() {
        configFile.reload();
        load();
    }

    private void load() {
        config = new GrammarConfig(configFile);
    }

    @Override
    public void process(@NonNull MessageContext ctx) {
        GrammarConfig cfg = config;
        if (cfg == null) return;

        Player player = ctx.getPlayer();
        if (Utils.hasPerms(player, BYPASS_PERM)) return;

        String original = ctx.getEffectiveRaw();
        String result = original;

        if (cfg.isTrimSpacesEnabled()) {
            result = GrammarProcessor.collapseSpaces(result);
        }

        if (cfg.isCapEnabled() && result.length() >= cfg.getCapMinCharacters()) {
            result = GrammarProcessor.applyCap(result, cfg.getCapLetters());
        }

        if (cfg.isSentenceCaseEnabled() && result.length() >= cfg.getSentenceCaseMinCharacters()) {
            result = GrammarProcessor.applySentenceCase(result);
        }

        if (cfg.isFinalDotEnabled() && result.length() >= cfg.getFinalDotMinCharacters()) {
            result = GrammarProcessor.applyFinalDot(result, cfg.getFinalDotCharacter(), cfg.getFinalDotIgnoredEndings());
        }

        if (!result.equals(original)) {
            ctx.setRawOverride(result);
        }
    }

    @Override
    public @NonNull String getId() { return ID; }
}