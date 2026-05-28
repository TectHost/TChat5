package tect.host.tpl.module.impl.chat.channel;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.MessagesManager;
import tect.host.tpl.context.MessageContext;
import tect.host.tpl.module.ModuleCommand;
import tect.host.tpl.module.registry.ModuleManager;
import tect.host.tpl.pipeline.ChatRenderer;
import tect.host.tpl.util.CompletionUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class ChannelCommand implements ModuleCommand {

    private static final String PERMISSION = "tchat.command.channel";

    private final ModuleManager moduleManager;
    private final MessagesManager messagesManager;

    public ChannelCommand(@NonNull ModuleManager moduleManager, @NonNull MessagesManager messagesManager) {
        this.moduleManager = moduleManager;
        this.messagesManager = messagesManager;
    }

    @Override
    public @NonNull String getName() { return "channel"; }

    @Contract(value = " -> new", pure = true)
    @Override
    public @NonNull @Unmodifiable List<String> getAliases() { return List.of("ch"); }

    @Override
    public void execute(@NonNull CommandSourceStack source, String @NonNull [] args) {
        CommandSender sender = source.getSender();

        if (!(sender instanceof Player player)) {
            messagesManager.sendMessage(sender, "command-player-only");
            return;
        }

        if (!player.hasPermission(PERMISSION)) {
            messagesManager.sendMessage(sender, "no-permission");
            return;
        }

        ChannelModule module = moduleManager.getModule(ChannelModule.ID, ChannelModule.class);
        if (module == null || module.getChannelService() == null) {
            messagesManager.sendMessage(sender, "channel-module-disabled");
            return;
        }

        ChannelService service = module.getChannelService();

        if (args.length == 0) {
            messagesManager.sendMessage(sender, "channel-usage");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "join" -> handleJoin(player, service, args);
            case "leave" -> handleLeave(player, service, args);
            case "send" -> handleSend(player, service, args);
            case "list" -> handleList(player, service);
            default -> messagesManager.sendMessage(sender, "channel-usage");
        }
    }

    private void handleJoin(@NonNull Player player, @NonNull ChannelService service, String @NonNull [] args) {
        if (args.length < 2) {
            messagesManager.sendMessage(player, "channel-join-usage");
            return;
        }

        String channelId = args[1];
        ChannelEntry channel = service.getChannel(channelId);
        if (channel == null) {
            messagesManager.sendMessage(player, "channel-not-found", Map.of("%channel%", channelId));
            return;
        }

        ChannelEntry current = service.getActiveChannel(player);
        if (current != null && !current.id().equals(channel.id())) {
            service.leave(player, current);
            messagesManager.sendMessage(player, "channel-left", Map.of("%channel%", current.id()));
            broadcastAnnounce(player, service, current, "channel-announce-leave");
        }

        switch (service.join(player, channel)) {
            case SUCCESS -> {
                messagesManager.sendMessage(player, "channel-joined", Map.of("%channel%", channel.id()));
                broadcastAnnounce(player, service, channel, "channel-announce-join");
            }
            case NO_PERMISSION -> messagesManager.sendMessage(player, "no-permission");
            case ALREADY_JOINED -> messagesManager.sendMessage(player, "channel-already-joined", Map.of("%channel%", channel.id()));
            case CHANNEL_FULL -> messagesManager.sendMessage(player, "channel-full", Map.of("%channel%", channel.id()));
        }
    }

    private void handleLeave(@NonNull Player player, @NonNull ChannelService service, String @NonNull [] args) {
        ChannelEntry channel;

        if (args.length >= 2) {
            channel = service.getChannel(args[1]);
            if (channel == null) {
                messagesManager.sendMessage(player, "channel-not-found", Map.of("%channel%", args[1]));
                return;
            }
        } else {
            channel = service.getActiveChannel(player);
            if (channel == null) {
                messagesManager.sendMessage(player, "channel-not-in-any");
                return;
            }
        }

        switch (service.leave(player, channel)) {
            case SUCCESS -> {
                messagesManager.sendMessage(player, "channel-left", Map.of("%channel%", channel.id()));
                broadcastAnnounce(player, service, channel, "channel-announce-leave");
            }
            case NOT_IN_CHANNEL -> messagesManager.sendMessage(player, "channel-not-in", Map.of("%channel%", channel.id()));
        }
    }

    private void handleSend(@NonNull Player player, @NonNull ChannelService service, String @NonNull [] args) {
        if (args.length < 3) {
            messagesManager.sendMessage(player, "channel-send-usage");
            return;
        }

        String channelId = args[1];
        ChannelEntry channel = service.getChannel(channelId);
        if (channel == null) {
            messagesManager.sendMessage(player, "channel-not-found", Map.of("%channel%", channelId));
            return;
        }

        String sendPerm = channel.permission() + ".send";
        if (!player.hasPermission(sendPerm) && !player.hasPermission(channel.permission())) {
            messagesManager.sendMessage(player, "no-permission");
            return;
        }

        String rawMessage = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        MessageContext msgCtx = new MessageContext(player, rawMessage, net.kyori.adventure.text.Component.text(rawMessage));

        Collection<? extends Player> online = moduleManager.getModuleContext().getOnlinePlayers();
        List<Player> recipients = service.resolveRecipients(channel, online);

        if (recipients.isEmpty()) {
            messagesManager.sendMessage(player, "channel-no-recipients", Map.of("%channel%", channel.id()));
            return;
        }

        Component rendered = ChatRenderer.render(
                moduleManager.getModuleContext().getPlaceholderApiHook(),
                player,
                channel.format().replace("%channel%", channel.id()),
                msgCtx
        );

        for (Player recipient : recipients) {
            recipient.sendMessage(rendered);
        }

        messagesManager.sendMessage(player, "channel-send-success", Map.of("%channel%", channel.id(), "%message%", rawMessage));
    }

    private void handleList(@NonNull Player player, @NonNull ChannelService service) {
        List<ChannelEntry> accessible = service.getChannels().stream()
                .filter(ch -> player.hasPermission(ch.permission()))
                .toList();

        if (accessible.isEmpty()) {
            messagesManager.sendMessage(player, "channel-list-empty");
            return;
        }

        ChannelEntry active = service.getActiveChannel(player);
        messagesManager.sendMessage(player, "channel-list-header");

        for (ChannelEntry ch : accessible) {
            boolean isActive = active != null && active.id().equals(ch.id());
            messagesManager.sendMessage(player, isActive ? "channel-list-entry-active" : "channel-list-entry", Map.of("%channel%", ch.id()));
        }
    }

    private void broadcastAnnounce(@NonNull Player player, @NonNull ChannelService service, @NonNull ChannelEntry channel, @NonNull String messageKey) {
        Collection<? extends Player> online = moduleManager.getModuleContext().getOnlinePlayers();
        List<Player> audience = service.resolveAnnounceAudience(channel, online);

        for (Player recipient : audience) {
            if (!recipient.equals(player)) {
                messagesManager.sendMessage(recipient, messageKey, Map.of("%player%",  player.getName(), "%channel%", channel.id()));
            }
        }
    }

    @Override
    public @NonNull Collection<String> suggest(@NonNull CommandSourceStack source, String @NonNull [] args) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player player)) return List.of();
        if (!player.hasPermission(PERMISSION)) return List.of();

        ChannelModule module = moduleManager.getModule(ChannelModule.ID, ChannelModule.class);
        if (module == null || module.getChannelService() == null) return List.of();
        ChannelService service = module.getChannelService();

        if (args.length <= 1) {
            return CompletionUtil.filterFrom(List.of("join", "leave", "send", "list"), args.length == 1 ? args[0] : "");
        }

        if (args.length == 2) {
            List<String> ids = service.getChannels().stream()
                    .filter(ch -> player.hasPermission(ch.permission()))
                    .map(ChannelEntry::id)
                    .toList();
            return CompletionUtil.filterFrom(ids, args[1]);
        }

        return List.of();
    }
}