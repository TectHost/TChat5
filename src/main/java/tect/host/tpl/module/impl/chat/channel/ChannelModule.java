package tect.host.tpl.module.impl.chat.channel;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.config.ConfigFile;
import tect.host.tpl.context.MessageContext;
import tect.host.tpl.context.MessageOrigin;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.type.ChatModule;

public final class ChannelModule implements ChatModule {

    public static final String ID = "channels";

    private final ModuleContext moduleContext;
    private ChannelService channelService;

    public ChannelModule(@NonNull ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    @Override
    public void onEnable() {
        ConfigFile channelsFile = moduleContext.createConfigFile("channels.yml", "modules");
        channelsFile.setMigrator(ChannelMigrations.create(moduleContext.getLogger()));
        this.channelService = new ChannelService(channelsFile);
        channelService.reload();
    }

    @Override
    public void onReload() {
        channelService.reload();
    }

    @Override
    public void onDisable() {
        channelService = null;
    }

    @Override
    public void process(@NonNull MessageContext msgCtx) {
        if (msgCtx.getOrigin() != MessageOrigin.CHAT) return;

        Player player = msgCtx.getPlayer();
        ChannelEntry channel = channelService.getActiveChannel(player);
        if (channel == null) return;

        if (channel.messageMode() == 3) {
            msgCtx.setCancelled(true);
            return;
        }

        channelService.sendToChannel(player, channel, msgCtx, moduleContext);
        msgCtx.setCancelled(true);
    }

    public @Nullable ChannelService getChannelService() {
        return channelService;
    }

    @Override
    public @NonNull String getId() { return ID; }
}