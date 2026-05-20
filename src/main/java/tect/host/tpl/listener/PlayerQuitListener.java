package tect.host.tpl.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.pipeline.QuitProcessor;
import tect.host.tpl.context.QuitContext;

public final class PlayerQuitListener implements Listener {

    private final QuitProcessor processor;

    public PlayerQuitListener(@NonNull QuitProcessor processor) {
        this.processor = processor;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NonNull PlayerQuitEvent event) {
        QuitContext ctx = new QuitContext(event.getPlayer());
        processor.process(ctx);
    }
}