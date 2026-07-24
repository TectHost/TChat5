package tect.host.tpl.listener;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.pipeline.ChatProcessor;
import tect.host.tpl.context.MessageContext;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

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

        Set<Player> playerViewers = new LinkedHashSet<>();
        for (Audience a : viewers) {
            if (a instanceof Player p) playerViewers.add(p);
        }

        MessageContext ctx = new MessageContext(player, rawMessage, original, playerViewers, event.signedMessage());

        processor.process(ctx);

        if (ctx.isCancelled()) { event.setCancelled(true); return; }

        Set<? extends Player> ctxRecipients = ctx.getRecipients();
        if (ctxRecipients != null) {
            viewers.removeIf(a -> a instanceof Player p && !ctxRecipients.contains(p));
        }

        if (ctx.hasRawOverride()) {
            event.message(Component.text(ctx.getEffectiveRaw()));
        } else if (!ctx.getMessage().equals(original)) {
            event.message(ctx.getMessage());
        }

        applyRenderer(event, ctx);
    }

    /**
     * Installs a renderer combining the pipeline's resolved format (if any) with
     * any per-viewer decorators registered by modules (e.g. a moderator-only
     * delete prefix). ChatRenderer is invoked once per viewer by Paper, so
     * decorators naturally see the correct recipient on each call.
     */
    private void applyRenderer(@NonNull AsyncChatEvent event, @NonNull MessageContext ctx) {
        Component format = ctx.getFormat();
        List<BiFunction<Player, Component, Component>> decorators = ctx.getViewerDecorators();

        if (format == null && decorators.isEmpty()) return;

        ChatRenderer fallback = format == null ? ChatRenderer.defaultRenderer() : null;

        event.renderer((source, displayName, message, viewer) -> {
            Component rendered = format != null ? format : fallback.render(source, displayName, message, viewer);

            if (viewer instanceof Player viewerPlayer) {
                for (BiFunction<Player, Component, Component> decorator : decorators) {
                    rendered = decorator.apply(viewerPlayer, rendered);
                }
            }

            return rendered;
        });
    }
}