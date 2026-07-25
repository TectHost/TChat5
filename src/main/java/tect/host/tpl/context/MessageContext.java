package tect.host.tpl.context;

import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;

public final class MessageContext {

    private final Player player;
    private @NonNull MessageOrigin origin = MessageOrigin.CHAT;
    private final World world;
    private final String rawMessage;
    private final @Nullable SignedMessage signedMessage;
    private @Nullable String overrideRaw;
    private Component message;
    private @Nullable Component format;
    private final @Nullable Set<? extends Player> recipients;
    private boolean cancelled;
    private final List<Runnable> onSuccessHooks = new ArrayList<>();
    private final List<BiFunction<Player, Component, Component>> viewerDecorators = new ArrayList<>();

    public MessageContext(@NonNull Player player, @NonNull String rawMessage, @NonNull Component message, @Nullable Set<? extends Player> recipients, @Nullable SignedMessage signedMessage) {
        this.player = Objects.requireNonNull(player, "player");
        this.world = player.getWorld();
        this.rawMessage = Objects.requireNonNull(rawMessage, "rawMessage");
        this.message = Objects.requireNonNull(message, "message");
        this.recipients = recipients;
        this.signedMessage = signedMessage;
    }

    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    public void setFormat(@NonNull Component format) { this.format = format; }
    public void setMessage(@NonNull Component message) { this.message = message; }
    public void setRawOverride(@NonNull String raw) { this.overrideRaw = raw; }
    public void setOrigin(@NonNull MessageOrigin origin) { this.origin = origin; }

    public @NonNull Player getPlayer() { return player; }
    public @NonNull World getWorld() { return world; }
    public @NonNull String getRawMessage() { return rawMessage; }
    public @NonNull Component getMessage() { return message; }
    public @Nullable Component getFormat() { return format; }
    public @Nullable Set<? extends Player> getRecipients() { return recipients; }
    public @Nullable SignedMessage getSignedMessage() { return signedMessage; }
    public @NonNull String getEffectiveRaw() { return overrideRaw != null ? overrideRaw : rawMessage; }
    public @NonNull MessageOrigin getOrigin() { return origin; }

    public boolean hasRawOverride() { return overrideRaw != null; }
    public boolean isCancelled() { return cancelled; }

    /**
     * Registers an action that runs only if the message reaches the end of the
     * pipeline without being cancelled by a later module
     */
    public void addOnSuccessHook(@NonNull Runnable hook) {
        onSuccessHooks.add(hook);
    }

    /** Called by the ChatProcessor after processing completes without cancellation */
    public void runOnSuccessHooks() {
        for (Runnable hook : onSuccessHooks) hook.run();
    }

    /**
     * Registers a per-viewer render decorator, applied to the final rendered
     * component individually for each recipient
     */
    public void addViewerDecorator(@NonNull BiFunction<Player, Component, Component> decorator) {
        viewerDecorators.add(Objects.requireNonNull(decorator, "decorator"));
    }

    @Contract(pure = true)
    public @NonNull @UnmodifiableView List<BiFunction<Player, Component, Component>> getViewerDecorators() {
        return Collections.unmodifiableList(viewerDecorators);
    }
}