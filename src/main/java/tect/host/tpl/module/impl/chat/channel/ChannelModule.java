package tect.host.tpl.module.impl.chat.channel;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.config.ConfigFile;
import tect.host.tpl.context.MessageContext;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.type.ChatModule;
import tect.host.tpl.pipeline.ChatRenderer;

import java.util.Collection;
import java.util.List;

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
        Player player = msgCtx.getPlayer();
        ChannelEntry channel = channelService.getActiveChannel(player);
        if (channel == null) return;

        if (channel.messageMode() == 3) {
            msgCtx.setCancelled(true);
            return;
        }

        Collection<? extends Player> online = moduleContext.getOnlinePlayers();
        List<Player> recipients = channelService.resolveRecipients(channel, online);

        Component formatted = ChatRenderer.render(
                moduleContext.getPlaceholderApiHook(),
                player,
                channel.format().replace("%channel%", channel.id()),
                msgCtx
        );
        msgCtx.setFormat(formatted);

        for (Player recipient : recipients) {
            recipient.sendMessage(formatted);
        }

        msgCtx.setCancelled(true);
    }

    public @Nullable ChannelService getChannelService() {
        return channelService;
    }

    @Override
    public @NonNull String getId() { return ID; }
}