package tect.host.tpl.util;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class MessageContext {
    private final Player player;
    private final String rawMessage;
    private Component message;
    private Component format;
    private boolean cancelled;

    public MessageContext(@NonNull Player player, @NonNull String rawMessage, @NonNull Component message) {
        this.player = Objects.requireNonNull(player, "player");
        this.rawMessage = Objects.requireNonNull(rawMessage, "rawMessage");
        this.message = Objects.requireNonNull(message, "message");
    }

    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    public void setFormat(@NonNull Component format) { this.format = format; }
    public void setMessage(@NonNull Component message) { this.message = message; }

    public @NonNull Player getPlayer() { return player; }
    public @NonNull String getRawMessage() { return rawMessage; }
    public @NonNull Component getMessage() { return message; }
    public @Nullable Component getFormat() { return format; }

    public boolean isCancelled() { return cancelled; }
}