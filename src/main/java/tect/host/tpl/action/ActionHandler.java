package tect.host.tpl.action;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface ActionHandler {
    void execute(@NonNull Player player, @NonNull String arg);
}