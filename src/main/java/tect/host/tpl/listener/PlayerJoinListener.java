package tect.host.tpl.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.manager.JoinProcessor;
import tect.host.tpl.util.JoinContext;

public final class PlayerJoinListener implements Listener {

    private final JoinProcessor processor;

    public PlayerJoinListener(@NonNull JoinProcessor processor) {
        this.processor = processor;
    }

    @EventHandler
    public void onJoin(@NonNull PlayerJoinEvent event) {
        JoinContext ctx = new JoinContext(event.getPlayer());
        processor.process(ctx);
    }
}