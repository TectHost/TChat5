package tect.host.tpl.manager;

import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.config.ConfigManager;
import tect.host.tpl.module.*;
import tect.host.tpl.module.Module;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class ModuleManager {

    private final Map<String, ModuleDescriptor> descriptors = new LinkedHashMap<>();
    private final Map<String, Module> activeModules = new LinkedHashMap<>();

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

    /**
     * Loads enabled modules with smart reload:
     * - Modules that were active and remain enabled receive onReload()
     * - New modules are created and receive onEnable()
     * - Modules no longer enabled are unloaded
     */
    public void loadEnabledModules() {
        Map<String, ModuleDescriptor> enabled = new LinkedHashMap<>();
        for (ModuleDescriptor descriptor : descriptors.values()) {
            if (configManager.isModuleEnabled(descriptor.getTogglePath())) {
                enabled.put(descriptor.getId(), descriptor);
            }
        }

        List<ModuleDescriptor> ordered = resolveDependencyOrder(enabled);

        // Determine which currently active modules must be unloaded
        Set<String> toUnload = new LinkedHashSet<>(activeModules.keySet());
        for (ModuleDescriptor d : ordered) {
            toUnload.remove(d.getId());
        }

        // Unload removed modules in reverse insertion order (avoid dependency errors)
        List<String> reverseUnload = new ArrayList<>(toUnload);
        Collections.reverse(reverseUnload);
        for (String id : reverseUnload) {
            Module module = activeModules.remove(id);
            if (module != null) {
                module.onDisable();
                logger.info("Module unloaded: %s".formatted(id));
            }
        }

        // Load or reload each module in dependency order
        for (ModuleDescriptor descriptor : ordered) {
            boolean hasMissing = descriptor.getRequiredModules().stream().anyMatch(dep -> !activeModules.containsKey(dep));

            if (hasMissing) {
                Set<String> missing = descriptor.getRequiredModules().stream()
                        .filter(dep -> !activeModules.containsKey(dep))
                        .collect(Collectors.toUnmodifiableSet());

                logger.warning("Module '%s' skipped: missing required modules %s".formatted(descriptor.getId(), missing));
                Module stale = activeModules.remove(descriptor.getId());
                if (stale != null) stale.onDisable();
                continue;
            }

            Module existing = activeModules.get(descriptor.getId());
            if (existing != null) {
                // If module already active -> smart reload
                try {
                    existing.onReload();
                    logger.info("Module reloaded: %s".formatted(descriptor.getId()));
                } catch (Exception e) {
                    logger.severe("Failed to reload module '%s': %s".formatted(descriptor.getId(), e.getMessage()));
                }
            } else {
                // If new module -> create and enable
                try {
                    Module module = descriptor.getFactory().create(moduleContext);
                    module.onEnable();
                    activeModules.put(descriptor.getId(), module);
                    logger.info("Module loaded: %s".formatted(descriptor.getId()));
                } catch (Exception e) {
                    logger.severe("Failed to load module '%s': %s".formatted(descriptor.getId(), e.getMessage()));
                }
            }
        }

        rebuildPipeline();
    }

    /**
     * I'm keeping this method in case I need to reload something else in the future
     * and maintain a consistent structure across the classes
     */
    public void reloadModules() {
        loadEnabledModules();
    }

    /**
     * activeModules preserves insertion order (LinkedHashMap) and modules
     * are inserted in topological order by loadEnabledModules(), so
     * reversing gives us correct teardown order (dependents before dependencies)
      */
    public void unloadAll() {
        List<Module> toUnload = new ArrayList<>(activeModules.values());
        Collections.reverse(toUnload);

        for (Module module : toUnload) {
            try {
                module.onDisable();
                logger.info("Module unloaded: %s".formatted(module.getId()));
            } catch (Exception e) {
                logger.severe("Error disabling module '%s': %s".formatted(module.getId(), e.getMessage()));
            }
        }

        activeModules.clear();
        this.pipeline = Pipeline.EMPTY;
    }

    public @NonNull @UnmodifiableView Collection<Module> getActiveModules() {
        return Collections.unmodifiableCollection(activeModules.values());
    }

    public List<ChatModule> getModulesForPhase(ModulePhase phase) {
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

    /**
     * I use Kahn's algorithm for topological sorting of module dependencies
     */
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
                if (inDegree.merge(dependent, -1, Integer::sum) == 0) {
                    queue.add(dependent);
                }
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
                    if (m instanceof ChatModule cm && desc != null && desc.getPhase() != null) {
                        snap.computeIfAbsent(desc.getPhase(), $ -> new ArrayList<>()).add(cm);
                    } else if (m instanceof JoinModule jm) {
                        newJoin.add(jm);
                    }
                });

        this.pipeline = new Pipeline(Map.copyOf(snap), List.copyOf(newJoin));
    }

    private int compareByPriorityThenId(Map.@NonNull Entry<String, Module> a, Map.@NonNull Entry<String, Module> b) {
        int pa = descriptors.get(a.getKey()).getPriority();
        int pb = descriptors.get(b.getKey()).getPriority();
        if (pa != pb) return Integer.compare(pa, pb);
        return a.getKey().compareTo(b.getKey());
    }
}