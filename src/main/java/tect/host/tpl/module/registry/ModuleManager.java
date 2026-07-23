package tect.host.tpl.module.registry;

import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.config.ConfigManager;
import tect.host.tpl.module.*;
import tect.host.tpl.module.Module;
import tect.host.tpl.module.type.*;
import tect.host.tpl.util.logging.DebugLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ModuleManager {

    private final Map<String, ModuleDescriptor> descriptors = new LinkedHashMap<>();
    private final ConcurrentHashMap<String, Module> activeModules = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ModuleCommand> activeCommands = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ModuleMenu> activeMenus = new ConcurrentHashMap<>();

    private record Pipeline(Map<ModulePhase, List<ChatModule>> chat, List<JoinModule> join, List<QuitModule> quit, List<CommandModule> commands) {
        static final Pipeline EMPTY = new Pipeline(Map.of(), List.of(), List.of(), List.of());
    }
    private volatile Pipeline pipeline = Pipeline.EMPTY;
    private volatile List<String> loadOrder = List.of();

    public record LoadResult(int loaded, int skipped, int failed) {
        static final LoadResult EMPTY = new LoadResult(0, 0, 0);
    }
    private volatile LoadResult lastLoadResult = LoadResult.EMPTY;

    public record MenuLoadResult(int enabled, int disabled, int skipped) {
        static final MenuLoadResult EMPTY = new MenuLoadResult(0, 0, 0);
    }
    private volatile MenuLoadResult lastMenuLoadResult = MenuLoadResult.EMPTY;

    public record ModuleTimings(double dependencyResolutionMs, double totalLoadMs, @Nullable String slowestModuleId, double slowestModuleMs) {
        static final ModuleTimings EMPTY = new ModuleTimings(0, 0, null, 0);
    }
    private volatile ModuleTimings lastTimings = ModuleTimings.EMPTY;

    private final DebugLogger log;
    private final ConfigManager configManager;
    private final ModuleContext moduleContext;

    public ModuleManager(@NonNull DebugLogger log, @NonNull ConfigManager configManager, @NonNull ModuleContext moduleContext) {
        this.log = log;
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
        final long totalStart = System.nanoTime();

        Map<String, ModuleDescriptor> enabled = new LinkedHashMap<>();
        for (ModuleDescriptor descriptor : descriptors.values()) {
            if (configManager.isModuleEnabled(descriptor.getTogglePath())) {
                enabled.put(descriptor.getId(), descriptor);
            }
        }

        final long dependencyStart = System.nanoTime();
        List<ModuleDescriptor> ordered = resolveDependencyOrder(enabled);
        final double dependencyMs = elapsedMs(dependencyStart);
        log.debug("Dependency resolution took %.2fms".formatted(dependencyMs));

        Set<String> toUnload = new LinkedHashSet<>(activeModules.keySet());
        for (ModuleDescriptor d : ordered) toUnload.remove(d.getId());

        List<String> reverseUnload = new ArrayList<>(loadOrder.reversed());
        reverseUnload.retainAll(toUnload);
        for (String id : toUnload) {
            if (!reverseUnload.contains(id)) reverseUnload.add(id);
        }

        for (String id : reverseUnload) disableAndRemove(id);

        int loaded = 0, skipped = 0, failed = 0;
        String slowestId = null;
        double slowestMs = -1;

        for (ModuleDescriptor descriptor : ordered) {
            Set<String> missing = descriptor.getRequiredModules().stream()
                    .filter(dep -> !activeModules.containsKey(dep))
                    .collect(Collectors.toUnmodifiableSet());

            if (!missing.isEmpty()) {
                log.warn("Module '%s' skipped: missing required modules %s".formatted(descriptor.getId(), missing));
                skipped++;
                continue;
            }

            final long moduleStart = System.nanoTime();

            Module existing = activeModules.get(descriptor.getId());
            if (existing != null) {
                if (reloadModule(descriptor, existing)) loaded++;
                else failed++;
            } else {
                boolean ok = enableModule(descriptor);
                if (ok) loaded++;
                else failed++;
            }

            double moduleMs = elapsedMs(moduleStart);
            log.debug("Module '%s' loaded in %.2fms".formatted(descriptor.getId(), moduleMs));
            if (moduleMs > slowestMs) {
                slowestMs = moduleMs;
                slowestId = descriptor.getId();
            }
        }

        loadOrder = ordered.stream().map(ModuleDescriptor::getId).toList();

        lastLoadResult = new LoadResult(loaded, skipped, failed);
        lastMenuLoadResult = resolveMenuLoadResult();
        rebuildPipeline();

        double totalMs = elapsedMs(totalStart);
        lastTimings = new ModuleTimings(dependencyMs, totalMs, slowestId, Math.max(slowestMs, 0));
        log.debug("Total module load took %.2fms".formatted(totalMs));
    }

    private @NonNull MenuLoadResult resolveMenuLoadResult() {
        int enabled = 0, disabled = 0, skipped = 0;
        for (ModuleDescriptor descriptor : descriptors.values()) {
            if (descriptor.getMenuFactory() == null) continue;
            if (!configManager.isModuleEnabled(descriptor.getTogglePath())) {
                disabled++;
            } else if (!activeModules.containsKey(descriptor.getId())) {
                skipped++;
            } else {
                enabled++;
            }
        }
        return new MenuLoadResult(enabled, disabled, skipped);
    }

    private static double elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }

    public void reloadModules() {
        log.refresh();
        loadEnabledModules();
    }

    public void unloadAll() {
        List<String> ids = new ArrayList<>(loadOrder.reversed());
        for (String id : activeModules.keySet()) {
            if (!ids.contains(id)) ids.add(id);
        }

        for (String id : ids) {
            Module module = activeModules.remove(id);
            activeCommands.remove(id);
            activeMenus.remove(id);
            if (module == null) continue;
            try {
                module.onDisable();
                log.debug("Module unloaded: " + id);
            } catch (Exception e) {
                log.severe("Error disabling module '%s': %s".formatted(id, e.getMessage()));
            }
        }

        pipeline = Pipeline.EMPTY;
        loadOrder = List.of();
    }

    private void disableAndRemove(@NonNull String id) {
        Module module = activeModules.remove(id);
        activeCommands.remove(id);
        activeMenus.remove(id);
        if (module == null) return;
        try {
            module.onDisable();
            log.debug("Module unloaded: " + id);
        } catch (Exception e) {
            log.severe("Error disabling module '%s': %s".formatted(id, e.getMessage()));
        }
    }

    private boolean reloadModule(@NonNull ModuleDescriptor descriptor, @NonNull Module module) {
        try {
            module.onReload();
            log.debug("Module reloaded: " + descriptor.getId());
            return true;
        } catch (Exception e) {
            log.severe("Failed to reload module '%s': %s".formatted(descriptor.getId(), e.getMessage()));
            return false;
        }
    }

    private boolean enableModule(@NonNull ModuleDescriptor descriptor) {
        Module module = null;
        try {
            module = descriptor.getFactory().create(moduleContext);

            if (!descriptor.getId().equals(module.getId())) {
                log.severe("Module registered under id '%s' but reports getId()='%s' — check ModuleRegistry".formatted(descriptor.getId(), module.getId()));
                return false;
            }

            module.onEnable();
            activeModules.put(descriptor.getId(), module);

            Function<ModuleManager, ModuleCommand> cmdFactory = descriptor.getCommandFactory();
            if (cmdFactory != null) activeCommands.put(descriptor.getId(), cmdFactory.apply(this));

            Function<ModuleManager, ModuleMenu> menuFactory = descriptor.getMenuFactory();
            if (menuFactory != null) activeMenus.put(descriptor.getId(), menuFactory.apply(this));

            log.debug("Module loaded: " + descriptor.getId());
            return true;
        } catch (Exception e) {
            log.severe("Failed to load module '%s': %s".formatted(descriptor.getId(), e.getMessage()));

            if (module != null) {
                try {
                    module.onDisable();
                } catch (Exception cleanupEx) {
                    log.severe("Cleanup after failed load also failed for '%s': %s".formatted(descriptor.getId(), cleanupEx.getMessage()));
                }
            }
            return false;
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
                dependents.computeIfAbsent(dep, _ -> new ArrayList<>()).add(d.getId());
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
            log.severe("Circular dependency detected, skipping modules: " + inCycle);
        }

        return ordered;
    }

    private void rebuildPipeline() {
        List<Map.Entry<String, Module>> sorted = new ArrayList<>(activeModules.entrySet());
        sorted.sort(this::compareByPriorityThenId);

        Map<ModulePhase, List<ChatModule>> chatSnap = new EnumMap<>(ModulePhase.class);
        List<JoinModule> newJoin = new ArrayList<>();
        List<QuitModule> newQuit = new ArrayList<>();
        List<CommandModule> newCommands = new ArrayList<>();

        for (Map.Entry<String, Module> entry : sorted) {
            Module m = entry.getValue();
            ModuleDescriptor desc = descriptors.get(entry.getKey());

            boolean recognised = false;

            if (m instanceof ChatModule cm) {
                recognised = true;
                if (desc == null || desc.getPhase() == null) {
                    log.warn("ChatModule '%s' has no phase, it will never execute!".formatted(entry.getKey()));
                } else {
                    chatSnap.computeIfAbsent(desc.getPhase(), _ -> new ArrayList<>()).add(cm);
                }
            }
            if (m instanceof JoinModule jm) {
                recognised = true;
                newJoin.add(jm);
            }
            if (m instanceof QuitModule qm) {
                recognised = true;
                newQuit.add(qm);
            }
            if (m instanceof CommandModule cm) {
                recognised = true;
                newCommands.add(cm);
            }
            if (m instanceof BroadcastModule) {
                recognised = true;
            }

            if (!recognised) {
                log.warn("Module '%s' has an unrecognised type and was not added to any pipeline: %s".formatted(entry.getKey(), m.getClass().getSimpleName()));
            }
        }

        pipeline = new Pipeline(Collections.unmodifiableMap(chatSnap), List.copyOf(newJoin), List.copyOf(newQuit), List.copyOf(newCommands));
    }

    private int compareByPriorityThenId(Map.@NonNull Entry<String, Module> a, Map.@NonNull Entry<String, Module> b) {
        int pa = descriptors.get(a.getKey()).getPriority();
        int pb = descriptors.get(b.getKey()).getPriority();
        return pa != pb ? Integer.compare(pa, pb) : a.getKey().compareTo(b.getKey());
    }

    public @NonNull ModuleContext getModuleContext() {
        return moduleContext;
    }

    public @NonNull MenuLoadResult getMenuLoadResult() { return lastMenuLoadResult; }

    public @NonNull ModuleTimings getModuleTimings() { return lastTimings; }

    public @NonNull LoadResult getLoadResult() { return lastLoadResult; }

    public @NonNull @UnmodifiableView Collection<ModuleCommand> getActiveCommands() {
        return Collections.unmodifiableCollection(activeCommands.values());
    }

    public @NonNull @UnmodifiableView Collection<ModuleMenu> getActiveMenus() {
        return Collections.unmodifiableCollection(activeMenus.values());
    }

    public @Nullable ModuleMenu getMenu(@NonNull String id) {
        return activeMenus.get(id);
    }

    public List<ChatModule> getModulesForPhase(@NonNull ModulePhase phase) {
        return pipeline.chat().getOrDefault(phase, List.of());
    }

    public List<JoinModule> getJoinModules() {
        return pipeline.join();
    }

    public List<QuitModule> getQuitModules() {
        return pipeline.quit();
    }

    public List<CommandModule> getCommandModules() {
        return pipeline.commands();
    }

    /**
     * Retrieve an active module by id, cast to the expected type
     * Returns null if the module is not loaded or the type doesn't match
     */
    @SuppressWarnings("unchecked")
    public <T extends Module> @Nullable T getModule(@NonNull String id, @NonNull Class<T> type) {
        Module module = activeModules.get(id);
        return type.isInstance(module) ? (T) module : null;
    }
}