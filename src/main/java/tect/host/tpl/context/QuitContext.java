package tect.host.tpl.context;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public final class QuitContext {

    private final Player player;

    public QuitContext(@NonNull Player player) {
        this.player = Objects.requireNonNull(player, "player");
    }

    public @NonNull Player getPlayer() { return player; }
}