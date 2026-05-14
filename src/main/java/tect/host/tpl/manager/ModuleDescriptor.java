package tect.host.tpl.manager;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.module.ModuleCommand;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.ModulePhase;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class ModuleDescriptor {

    private final String id;
    private final String togglePath;
    private final ModuleFactory factory;
    private final Set<String> requiredModules;
    private final @Nullable ModulePhase phase;
    private final int priority;
    private final @Nullable Function<ModuleManager, ModuleCommand> commandFactory;

    private ModuleDescriptor(@NonNull Builder builder) {
        this.id = builder.id;
        this.togglePath = builder.togglePath;
        this.factory = builder.factory;
        this.requiredModules = Set.copyOf(builder.requiredModules);
        this.phase = builder.phase;
        this.priority = builder.priority;
        this.commandFactory = builder.commandFactory;
    }

    @Contract("_, _, _ -> new")
    public static @NonNull Builder builder(@NonNull String id, @NonNull String togglePath, @NonNull ModuleFactory factory) {
        return new Builder(id, togglePath, factory);
    }

    public @NonNull String getId() { return id; }
    public @NonNull String getTogglePath() { return togglePath; }
    public @NonNull ModuleFactory getFactory() { return factory; }
    public @NonNull Set<String> getRequiredModules() { return requiredModules; }
    public @Nullable ModulePhase getPhase() { return phase; }
    public int getPriority() { return priority; }

    public @Nullable Function<ModuleManager, ModuleCommand> getCommandFactory() {
        return commandFactory;
    }

    public static final class Builder {

        private final String id;
        private final String togglePath;
        private final ModuleFactory factory;
        private Set<String> requiredModules = Set.of();
        private @Nullable ModulePhase phase = null;
        private int priority = 100;
        private @Nullable Function<ModuleManager, ModuleCommand> commandFactory = null;

        private Builder(String id, String togglePath, ModuleFactory factory) {
            this.id = id;
            this.togglePath = togglePath;
            this.factory = factory;
        }

        public Builder requires(@NonNull String... moduleIds) {
            this.requiredModules = Set.of(moduleIds);
            return this;
        }

        public Builder phase(@NonNull ModulePhase phase) {
            this.phase = phase;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder command(@NonNull Function<ModuleManager, ModuleCommand> commandFactory) {
            this.commandFactory = commandFactory;
            return this;
        }

        @Contract(" -> new")
        public @NonNull ModuleDescriptor build() {
            return new ModuleDescriptor(this);
        }
    }
}