package tect.host.tpl.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.manager.ChatProcessor;
import tect.host.tpl.util.MessageContext;

public final class ChatListener implements Listener {

    private final ChatProcessor processor;

    public ChatListener(ChatProcessor processor) {
        this.processor = processor;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(@NonNull AsyncChatEvent event) {
        Player player = event.getPlayer();
        Component original = event.message();

        String rawMessage = event.signedMessage().message();

        MessageContext ctx = new MessageContext(player, rawMessage, original);

        processor.process(ctx);

        if (ctx.isCancelled()) { event.setCancelled(true); return; }

        if (ctx.getFormat() != null) {
            event.renderer((source, dn, msg, viewer) -> ctx.getFormat());
        } else if (!ctx.getMessage().equals(original)) {
            event.message(ctx.getMessage());
        }

        // If no module sets the format, Paper uses its default renderer
        // This is the expected behavior when all formatting modules are disabled
    }
}