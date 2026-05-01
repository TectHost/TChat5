package tect.host.tpl.module;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.TimeUnit;

public final class BukkitSchedulerAccess implements SchedulerAccess {

    private final JavaPlugin plugin;

    public BukkitSchedulerAccess(@NonNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NonNull Cancellable runAsync(@NonNull Runnable task) {
        ScheduledTask t = plugin.getServer().getAsyncScheduler().runNow(plugin, $ -> task.run());
        return t::cancel;
    }

    @Override
    public @NonNull Cancellable runSync(@NonNull Runnable task) {
        ScheduledTask t = plugin.getServer().getGlobalRegionScheduler().run(plugin, $ -> task.run());
        return t::cancel;
    }

    /**
     * runLater is always async
     */
    @Override
    public @NonNull Cancellable runLater(@NonNull Runnable task, long delayTicks) {
        long delayNanos = delayTicks * 50L * 1_000_000L;
        ScheduledTask t = plugin.getServer().getAsyncScheduler().runDelayed(plugin, $ -> task.run(), delayNanos, TimeUnit.NANOSECONDS);
        return t::cancel;
    }

    @Override
    public @NonNull Cancellable runTimer(@NonNull Runnable task, long delayTicks, long periodTicks) {
        long delayNanos  = delayTicks  * 50L * 1_000_000L;
        long periodNanos = periodTicks * 50L * 1_000_000L;
        ScheduledTask t = plugin.getServer().getAsyncScheduler().runAtFixedRate(plugin, $ -> task.run(), delayNanos, periodNanos, TimeUnit.NANOSECONDS);
        return t::cancel;
    }
}