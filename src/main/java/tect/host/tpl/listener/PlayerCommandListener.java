package tect.host.tpl.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.pipeline.CommandProcessor;
import tect.host.tpl.context.CommandContext;

public final class PlayerCommandListener implements Listener {

    private final CommandProcessor commandProcessor;

    public PlayerCommandListener(CommandProcessor commandProcessor) {
        this.commandProcessor = commandProcessor;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCommand(@NonNull PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        CommandContext ctx = new CommandContext(player, event.getMessage());

        if (!commandProcessor.process(ctx)) {
            event.setCancelled(true);
            return;
        }

        if (ctx.hasRedirect()) {
            event.setMessage(ctx.getEffectiveCommand());
        }
    }
}