package tect.host.tpl.module.impl.chat.anticap;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigFile;
import tect.host.tpl.module.type.ChatModule;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.util.ColorUtil;
import tect.host.tpl.context.MessageContext;
import tect.host.tpl.util.Utils;

import java.util.List;

public final class AntiCapModule implements ChatModule {

    private static final String ID = "anti-cap";
    private static final String BYPASS_PERM = "tchat.admin.bypass.anticap";

    private record LoadedState(@NonNull AntiCapConfig config, @NonNull List<Component> messageComponents) {
        @Contract("_ -> new")
        static @NonNull LoadedState from(@NonNull AntiCapConfig config) {
            List<Component> components = config.getMessage().stream()
                    .map(ColorUtil::legacyToMini)
                    .map(ColorUtil::deserialize)
                    .toList();
            return new LoadedState(config, components);
        }
    }

    private final ModuleContext moduleContext;
    private ConfigFile configFile;

    private volatile LoadedState state;

    public AntiCapModule(@NonNull ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    @Override
    public void onEnable() {
        configFile = moduleContext.createConfigFile("anticap.yml", "modules");
        configFile.setMigrator(AntiCapMigrations.create(moduleContext.getLogger()));
        configFile.register();
        load();
    }

    @Override
    public void onReload() {
        configFile.reload();
        load();
    }

    private void load() {
        state = LoadedState.from(new AntiCapConfig(configFile));
    }

    @Override
    public void process(@NonNull MessageContext ctx) {
        LoadedState snap = state;
        if (snap == null) return;
        if (Utils.hasPerms(ctx.getPlayer(), BYPASS_PERM)) return;

        String raw = ctx.getRawMessage();
        if (!exceedsCapThreshold(raw, snap.config().getPercent())) return;

        switch (snap.config().getAction()) {
            case BLOCK -> {
                ctx.setCancelled(true);

                List<Component> components = snap.messageComponents();
                if (!components.isEmpty()) {
                    Player player = ctx.getPlayer();
                    for (Component component : components) {
                        player.sendMessage(component);
                    }
                }
            }
            case CENSOR -> ctx.setRawOverride(censorUpperCase(raw, snap.config().getCensorChar()));
            case TO_LOWER_CASE -> ctx.setRawOverride(raw.toLowerCase(java.util.Locale.ROOT));
        }
    }

    /**
     * Returns true when the ratio of uppercase letters to total letters
     * meets or exceeds the configured threshold
     */
    private static boolean exceedsCapThreshold(@NonNull String message, double percent) {
        int letters = 0;
        int upper = 0;

        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (Character.isLetter(c)) {
                letters++;
                if (Character.isUpperCase(c)) upper++;
            }
        }

        if (letters < 4) return false;
        return (double) upper / letters >= percent;
    }

    /** Replaces every uppercase letter with the censorChar, leaving other characters untouched */
    private static @NonNull String censorUpperCase(@NonNull String message, char censorChar) {
        char[] chars = message.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (Character.isUpperCase(chars[i])) chars[i] = censorChar;
        }
        return new String(chars);
    }

    @Override
    public @NonNull String getId() { return ID; }
}