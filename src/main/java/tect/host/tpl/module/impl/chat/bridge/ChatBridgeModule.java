package tect.host.tpl.module.impl.chat.bridge;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigFile;
import tect.host.tpl.context.MessageContext;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.type.ChatModule;

import java.util.Set;

public final class ChatBridgeModule implements ChatModule {

    private static final String ID = "chat-bridge";

    private final ModuleContext moduleContext;
    private ConfigFile configFile;

    private volatile ChatBridgeConfig config;

    public ChatBridgeModule(@NonNull ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    @Override
    public void onEnable() {
        configFile = moduleContext.createConfigFile("chatbridge.yml", "modules");
        configFile.setMigrator(ChatBridgeMigrations.create(moduleContext.getLogger()));
        configFile.register();
        load();
    }

    @Override
    public void onReload() {
        configFile.reload();
        load();
    }

    private void load() {
        config = new ChatBridgeConfig(configFile);
    }

    @Override
    public void process(@NonNull MessageContext ctx) {
        ChatBridgeConfig snap = config;
        if (snap == null) return;

        String senderWorld = ctx.getWorld().getName();
        BridgeEntry bridge = snap.bridgeForWorld(senderWorld);

        if (bridge == null) return;

        Set<? extends org.bukkit.entity.Player> recipients = ctx.getRecipients();
        if (recipients == null) return;

        Set<String> bridgeWorlds = bridge.worlds();
        recipients.removeIf(p -> !bridgeWorlds.contains(p.getWorld().getName()));
    }

    @Override
    public @NonNull String getId() { return ID; }
}