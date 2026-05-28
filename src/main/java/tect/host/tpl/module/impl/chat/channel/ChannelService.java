package tect.host.tpl.module.impl.chat.channel;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.config.ConfigFile;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ChannelService {

    private final ConfigFile channelsFile;

    private volatile List<ChannelEntry> channels = List.of();

    private final ConcurrentHashMap<UUID, String> playerChannel = new ConcurrentHashMap<>();

    public ChannelService(@NonNull ConfigFile channelsFile) {
        this.channelsFile = channelsFile;
        this.channelsFile.register();
    }

    public void reload() {
        channelsFile.reload();
        this.channels = new ChannelModuleConfig(channelsFile.get()).getChannels();
    }

    public JoinResult join(@NonNull Player player, @NonNull ChannelEntry channel) {
        if (!player.hasPermission(channel.permission())) {
            return JoinResult.NO_PERMISSION;
        }

        String current = playerChannel.get(player.getUniqueId());
        if (channel.id().equals(current)) {
            return JoinResult.ALREADY_JOINED;
        }

        if (channel.hasLimit()) {
            long count = playerChannel.values().stream().filter(id -> id.equals(channel.id())).count();
            if (count >= channel.limit()) {
                return JoinResult.CHANNEL_FULL;
            }
        }

        playerChannel.put(player.getUniqueId(), channel.id());
        return JoinResult.SUCCESS;
    }

    public LeaveResult leave(@NonNull Player player, @NonNull ChannelEntry channel) {
        String current = playerChannel.get(player.getUniqueId());
        if (!channel.id().equals(current)) {
            return LeaveResult.NOT_IN_CHANNEL;
        }
        playerChannel.remove(player.getUniqueId());
        return LeaveResult.SUCCESS;
    }

    /**
     * Returns all online players currently joined to the given channel
     */
    public @NonNull List<Player> getMembersOf(@NonNull ChannelEntry channel, @NonNull Collection<? extends Player> onlinePlayers) {
        List<Player> members = new ArrayList<>();
        for (Player p : onlinePlayers) {
            if (channel.id().equals(playerChannel.get(p.getUniqueId()))) {
                members.add(p);
            }
        }
        return members;
    }

    /**
     * Resolves the recipient list for a channel message based on its messageMode.
     * Centralizes the logic that was previously duplicated in ChannelModule and ChannelCommand
     */
    public @NonNull List<Player> resolveRecipients(@NonNull ChannelEntry channel, @NonNull Collection<? extends Player> onlinePlayers) {
        return switch (channel.messageMode()) {
            case 0  -> new ArrayList<>(onlinePlayers);
            case 1  -> onlinePlayers.stream()
                    .filter(p -> p.hasPermission(channel.permission()))
                    .map(p -> (Player) p)
                    .toList();
            case 2  -> getMembersOf(channel, onlinePlayers);
            default -> List.of();
        };
    }

    /**
     * Resolves the audience for a join/leave announcement based on announceMode
     */
    public @NonNull List<Player> resolveAnnounceAudience(@NonNull ChannelEntry channel, @NonNull Collection<? extends Player> onlinePlayers) {
        return switch (channel.announceMode()) {
            case 0  -> new ArrayList<>(onlinePlayers);
            case 1  -> onlinePlayers.stream()
                    .filter(p -> p.hasPermission(channel.permission()))
                    .map(p -> (Player) p)
                    .toList();
            case 2  -> getMembersOf(channel, onlinePlayers);
            default -> List.of();
        };
    }

    public @NonNull List<ChannelEntry> getChannels() {
        return channels;
    }

    public @Nullable ChannelEntry getChannel(@NonNull String id) {
        for (ChannelEntry entry : channels) {
            if (entry.id().equalsIgnoreCase(id)) return entry;
        }
        return null;
    }

    public @Nullable ChannelEntry getActiveChannel(@NonNull Player player) {
        String id = playerChannel.get(player.getUniqueId());
        return id != null ? getChannel(id) : null;
    }
}