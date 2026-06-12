package tect.host.tpl.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.pipeline.ChatProcessor;
import tect.host.tpl.context.MessageContext;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class PlayerChatListener implements Listener {

    private final ChatProcessor processor;

    public PlayerChatListener(ChatProcessor processor) {
        this.processor = processor;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(@NonNull AsyncChatEvent event) {
        Player player = event.getPlayer();
        Component original = event.message();
        String rawMessage = event.signedMessage().message();

        Set<Audience> viewers = event.viewers();

        MessageContext ctx = new MessageContext(player, rawMessage, original, (Set<? extends Player>) event.viewers()
                .stream()
                .filter(a -> a instanceof Player)
                .map(a -> (Player) a)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        processor.process(ctx);

        if (ctx.isCancelled()) { event.setCancelled(true); return; }

        Set<? extends Player> ctxRecipients = ctx.getRecipients();
        if (ctxRecipients != null) {
            viewers.removeIf(a -> a instanceof Player p && !ctxRecipients.contains(p));
        }

        Component format = ctx.getFormat();
        if (format != null) {
            event.renderer((source, dn, msg, viewer) -> format);
        } else if (ctx.hasRawOverride()) {
            event.message(Component.text(ctx.getEffectiveRaw()));
        } else if (!ctx.getMessage().equals(original)) {
            event.message(ctx.getMessage());
        }
    }
}