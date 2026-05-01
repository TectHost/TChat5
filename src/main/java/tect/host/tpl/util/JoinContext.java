package tect.host.tpl.util;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public final class JoinContext {

    private final Player player;
    private boolean cancelled;

    public JoinContext(@NonNull Player player) {
        this.player = Objects.requireNonNull(player, "player");
    }

    public @NonNull Player getPlayer() { return player; }
    public boolean isCancelled() { return cancelled; }
    public void cancel() { this.cancelled = true; }
}