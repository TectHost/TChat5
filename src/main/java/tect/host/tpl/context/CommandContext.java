package tect.host.tpl.context;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CommandContext {

    private final Player player;
    private final String rawCommand;
    private @Nullable String redirect;
    private boolean cancelled;
    private final List<Runnable> onSuccessHooks = new ArrayList<>();

    public CommandContext(@NonNull Player player, @NonNull String rawCommand) {
        this.player = Objects.requireNonNull(player, "player");
        this.rawCommand = Objects.requireNonNull(rawCommand, "rawCommand");
    }

    public void cancel() {
        this.cancelled = true;
    }

    /**
     * Replaces the command that will be executed
     * The value must start with '/'
     */
    public void setRedirect(@Nullable String redirect) {
        if (redirect != null && !redirect.startsWith("/")) {
            throw new IllegalArgumentException("redirect must start with '/': " + redirect);
        }
        this.redirect = redirect;
    }


    public @NonNull Player getPlayer() { return player; }
    public @NonNull String getRawCommand() { return rawCommand; }
    public @NonNull String getEffectiveCommand() {
        return redirect != null ? redirect : rawCommand;
    }
    public boolean hasRedirect() { return redirect != null; }
    public boolean isCancelled() { return cancelled; }

    /**
     * Registers an action that runs only if the message reaches the end of the
     * pipeline without being cancelled by a later module (same as MessageContext)
     */
    public void addOnSuccessHook(@NonNull Runnable hook) {
        onSuccessHooks.add(hook);
    }

    public void runOnSuccessHooks() {
        for (Runnable hook : onSuccessHooks) hook.run();
    }
}