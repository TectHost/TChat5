package tect.host.tpl.module;

import org.jspecify.annotations.NonNull;

public interface SchedulerAccess {

    @FunctionalInterface
    interface Cancellable {
        void cancel();
    }

    Cancellable runAsync(@NonNull Runnable task);
    Cancellable runSync(@NonNull Runnable task);
    Cancellable runLater(@NonNull Runnable task, long delayTicks);
    Cancellable runTimer(@NonNull Runnable task, long delayTicks, long periodTicks);
}