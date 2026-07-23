package tect.host.tpl.module.impl.chat.chatcooldown;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.action.CompiledActions;
import tect.host.tpl.config.ConfigFile;
import tect.host.tpl.context.MessageContext;
import tect.host.tpl.context.QuitContext;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.type.ChatModule;
import tect.host.tpl.module.type.QuitModule;
import tect.host.tpl.util.Utils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatCooldownModule implements ChatModule, QuitModule {

    private static final String ID = "chat-cooldown";

    private static final String BYPASS_PERM = "tchat.admin.bypass.chatcooldown";
    private static final String PLACEHOLDER_REMAINING = "%cooldown_remaining%";

    private final ModuleContext moduleContext;
    private ConfigFile configFile;

    private volatile @Nullable ChatCooldownConfig config;
    private volatile CompiledActions compiledActions = CompiledActions.EMPTY;

    private final Map<UUID, Long> lastMessageAt = new ConcurrentHashMap<>();

    public ChatCooldownModule(@NonNull ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    @Override
    public void onEnable() {
        configFile = moduleContext.createConfigFile("chatcooldown.yml", "modules");
        configFile.setMigrator(ChatCooldownMigrations.create(moduleContext.getLogger()));
        configFile.register();
        load();
    }

    @Override
    public void onReload() {
        configFile.reload();
        load();
    }

    private void load() {
        ChatCooldownConfig cfg = new ChatCooldownConfig(configFile);
        config = cfg;
        compiledActions = moduleContext.getActionExecutor().compile(cfg.getActions());
    }

    @Override
    public void process(@NonNull MessageContext ctx) {
        ChatCooldownConfig cfg = config;
        if (cfg == null || cfg.getCooldownMillis() <= 0) return;

        Player player = ctx.getPlayer();
        if (Utils.hasPerms(player, BYPASS_PERM)) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastMessageAt.get(uuid);

        if (last != null) {
            long elapsed = now - last;
            if (elapsed < cfg.getCooldownMillis()) {
                long remainingSeconds = Math.floorDiv(cfg.getCooldownMillis() - elapsed + 999, 1000L);
                moduleContext.getActionExecutor().execute(player, compiledActions, Map.of(PLACEHOLDER_REMAINING, String.valueOf(remainingSeconds)));
                ctx.setCancelled(true);
                return;
            }
        }

        ctx.addOnSuccessHook(() -> lastMessageAt.put(uuid, now));
    }

    @Override
    public void onQuit(@NonNull QuitContext quitCtx) {
        lastMessageAt.remove(quitCtx.getPlayer().getUniqueId());
    }

    @Override
    public @NonNull String getId() { return ID; }
}