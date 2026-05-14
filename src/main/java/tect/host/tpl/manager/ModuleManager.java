package tect.host.tpl.manager;

import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.config.ConfigManager;
import tect.host.tpl.module.*;
import tect.host.tpl.module.Module;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class ModuleManager {

    private final Map<String, ModuleDescriptor> descriptors = new LinkedHashMap<>();
    private final ConcurrentHashMap<String, Module> activeModules = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ModuleCommand> activeCommands = new ConcurrentHashMap<>();

    private record Pipeline(Map<ModulePhase, List<ChatModule>> chat, List<JoinModule> join) {
        static final Pipeline EMPTY = new Pipeline(Map.of(), List.of());
    }
    private volatile Pipeline pipeline = Pipeline.EMPTY;

    private final Logger logger;
    private final ConfigManager configManager;
    private final ModuleContext moduleContext;

    public ModuleManager(Logger logger, ConfigManager configManager, ModuleContext moduleContext) {
        this.logger = logger;
        this.configManager = configManager;
        this.moduleContext = moduleContext;
    }

    public void registerDescriptor(@NonNull ModuleDescriptor descriptor) {
        descriptors.put(descriptor.getId(), descriptor);
    }

    public void registerDescriptors(@NonNull Collection<ModuleDescriptor> moduleDescriptors) {
        moduleDescriptors.forEach(this::registerDescriptor);
    }

    public void loadEnabledModules() {
        Map<String, ModuleDescriptor> enabled = new LinkedHashMap<>();
        for (ModuleDescriptor descriptor : descriptors.values()) {
            if (configManager.isModuleEnabled(descriptor.getTogglePath())) {
                enabled.put(descriptor.getId(), descriptor);
            }
        }

        List<ModuleDescriptor> ordered = resolveDependencyOrder(enabled);

        Set<String> toUnload = new LinkedHashSet<>(activeModules.keySet());
        for (ModuleDescriptor d : ordered) toUnload.remove(d.getId());

        List<String> reverseUnload = new ArrayList<>(toUnload);
        Collections.reverse(reverseUnload);

        for (String id : reverseUnload) disableAndRemove(id);

        for (ModuleDescriptor descriptor : ordered) {
            Set<String> missing = descriptor.getRequiredModules().stream()
                    .filter(dep -> !activeModules.containsKey(dep))
                    .collect(Collectors.toUnmodifiableSet());

            if (!missing.isEmpty()) {
                logger.warning("Module '%s' skipped: missing required modules %s".formatted(descriptor.getId(), missing));
                disableAndRemove(descriptor.getId());
                continue;
            }

            Module existing = activeModules.get(descriptor.getId());
            if (existing != null) {
                reloadModule(descriptor, existing);
            } else {
                enableModule(descriptor);
            }
        }

        rebuildPipeline();
    }

    public void reloadModules() {
        loadEnabledModules();
    }

    public void unloadAll() {
        List<String> ids = new ArrayList<>(activeModules.keySet());
        Collections.reverse(ids);

        for (String id : ids) {
            Module module = activeModules.remove(id);
            activeCommands.remove(id);
            if (module == null) continue;
            try {
                module.onDisable();
                logger.info("Module unloaded: %s".formatted(id));
            } catch (Exception e) {
                logger.severe("Error disabling module '%s': %s".formatted(id, e.getMessage()));
            }
        }

        pipeline = Pipeline.EMPTY;
    }

    public @NonNull ModuleContext getModuleContext() {
        return moduleContext;
    }

    public @NonNull @UnmodifiableView Collection<Module> getActiveModules() {
        return Collections.unmodifiableCollection(activeModules.values());
    }

    public @NonNull @UnmodifiableView Collection<ModuleCommand> getActiveCommands() {
        return Collections.unmodifiableCollection(activeCommands.values());
    }

    public List<ChatModule> getModulesForPhase(@NonNull ModulePhase phase) {
        return pipeline.chat().getOrDefault(phase, List.of());
    }

    public List<JoinModule> getJoinModules() {
        return pipeline.join();
    }

    /**
     * Retrieve an active module by id, cast to the expected type
     * Returns null if the module is not loaded or type doesn't match
     */
    @SuppressWarnings("unchecked")
    public <T extends Module> @Nullable T getModule(@NonNull String id, @NonNull Class<T> type) {
        Module module = activeModules.get(id);
        return type.isInstance(module) ? (T) module : null;
    }

    private void disableAndRemove(@NonNull String id) {
        Module module = activeModules.remove(id);
        activeCommands.remove(id);
        if (module == null) return;
        try {
            module.onDisable();
            logger.info("Module unloaded: %s".formatted(id));
        } catch (Exception e) {
            logger.severe("Error disabling module '%s': %s".formatted(id, e.getMessage()));
        }
    }

    private void reloadModule(@NonNull ModuleDescriptor descriptor, @NonNull Module module) {
        try {
            module.onReload();
            logger.info("Module reloaded: %s".formatted(descriptor.getId()));
        } catch (Exception e) {
            logger.severe("Failed to reload module '%s': %s".formatted(descriptor.getId(), e.getMessage()));
        }
    }

    private void enableModule(@NonNull ModuleDescriptor descriptor) {
        try {
            Module module = descriptor.getFactory().create(moduleContext);
            module.onEnable();
            activeModules.put(descriptor.getId(), module);

            Function<ModuleManager, ModuleCommand> cmdFactory = descriptor.getCommandFactory();
            if (cmdFactory != null) {
                activeCommands.put(descriptor.getId(), cmdFactory.apply(this));
            }

            logger.info("Module loaded: %s".formatted(descriptor.getId()));
        } catch (Exception e) {
            logger.severe("Failed to load module '%s': %s".formatted(descriptor.getId(), e.getMessage()));
        }
    }

    /** Kahn's algorithm, topological sort for dependency ordering */
    private @NonNull List<ModuleDescriptor> resolveDependencyOrder(@NonNull Map<String, ModuleDescriptor> enabled) {
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        Map<String, List<String>> dependents = new HashMap<>();

        for (ModuleDescriptor d : enabled.values()) {
            inDegree.putIfAbsent(d.getId(), 0);
            for (String dep : d.getRequiredModules()) {
                if (!enabled.containsKey(dep)) continue;
                dependents.computeIfAbsent(dep, k -> new ArrayList<>()).add(d.getId());
                inDegree.merge(d.getId(), 1, Integer::sum);
            }
        }

        Queue<String> queue = new ArrayDeque<>();
        inDegree.forEach((id, degree) -> { if (degree == 0) queue.add(id); });

        List<ModuleDescriptor> ordered = new ArrayList<>();
        while (!queue.isEmpty()) {
            String id = queue.poll();
            ModuleDescriptor descriptor = enabled.get(id);
            if (descriptor != null) ordered.add(descriptor);
            for (String dependent : dependents.getOrDefault(id, List.of())) {
                if (inDegree.merge(dependent, -1, Integer::sum) == 0) queue.add(dependent);
            }
        }

        if (ordered.size() < enabled.size()) {
            Set<String> inCycle = new HashSet<>(enabled.keySet());
            ordered.forEach(d -> inCycle.remove(d.getId()));
            logger.severe("Circular dependency detected, skipping modules: %s".formatted(inCycle));
        }

        return ordered;
    }

    private void rebuildPipeline() {
        Map<ModulePhase, List<ChatModule>> snap = new EnumMap<>(ModulePhase.class);
        List<JoinModule> newJoin = new ArrayList<>();

        activeModules.entrySet().stream()
                .sorted(this::compareByPriorityThenId)
                .forEach(e -> {
                    Module m = e.getValue();
                    ModuleDescriptor desc = descriptors.get(e.getKey());

                    if (m instanceof ChatModule cm) {
                        if (desc == null || desc.getPhase() == null) {
                            logger.warning("ChatModule '%s' has no phase — it will never execute!".formatted(e.getKey()));
                        } else {
                            snap.computeIfAbsent(desc.getPhase(), $ -> new ArrayList<>()).add(cm);
                        }
                    } else if (m instanceof JoinModule jm) {
                        newJoin.add(jm);
                    }
                });

        pipeline = new Pipeline(Collections.unmodifiableMap(snap), List.copyOf(newJoin));
    }

    private int compareByPriorityThenId(Map.@NonNull Entry<String, Module> a, Map.@NonNull Entry<String, Module> b) {
        int pa = descriptors.get(a.getKey()).getPriority();
        int pb = descriptors.get(b.getKey()).getPriority();
        return pa != pb ? Integer.compare(pa, pb) : a.getKey().compareTo(b.getKey());
    }
}