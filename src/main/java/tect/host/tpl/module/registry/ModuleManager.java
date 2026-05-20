package tect.host.tpl.module.registry;

import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.config.ConfigManager;
import tect.host.tpl.module.*;
import tect.host.tpl.module.Module;
import tect.host.tpl.module.type.ChatModule;
import tect.host.tpl.module.type.CommandModule;
import tect.host.tpl.module.type.JoinModule;
import tect.host.tpl.module.type.QuitModule;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class ModuleManager {

    private final Map<String, ModuleDescriptor> descriptors = new LinkedHashMap<>();
    private final ConcurrentHashMap<String, Module> activeModules = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ModuleCommand> activeCommands = new ConcurrentHashMap<>();

    private record Pipeline(Map<ModulePhase, List<ChatModule>> chat, List<JoinModule> join, List<QuitModule> quit, List<CommandModule> commands) {
        static final Pipeline EMPTY = new Pipeline(Map.of(), List.of(), List.of(), List.of());
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
            logger.severe("Circular dependency detected, skipping modules: %s".formatted(inCycle));
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

            switch (m) {
                case ChatModule cm -> {
                    if (desc == null || desc.getPhase() == null) {
                        logger.warning("ChatModule '%s' has no phase, it will never execute!".formatted(entry.getKey())
                        );
                    } else {
                        chatSnap.computeIfAbsent(desc.getPhase(), _ -> new ArrayList<>()).add(cm);
                    }
                }
                case JoinModule jm -> newJoin.add(jm);
                case QuitModule qm -> newQuit.add(qm);
                case CommandModule cm -> newCommands.add(cm);
                default -> throw new IllegalStateException("Unexpected value: " + m);
            }
        }

        pipeline = new Pipeline(
                Collections.unmodifiableMap(chatSnap),
                List.copyOf(newJoin),
                List.copyOf(newQuit),
                List.copyOf(newCommands)
        );
    }

    private int compareByPriorityThenId(Map.@NonNull Entry<String, Module> a, Map.@NonNull Entry<String, Module> b) {
        int pa = descriptors.get(a.getKey()).getPriority();
        int pb = descriptors.get(b.getKey()).getPriority();
        return pa != pb ? Integer.compare(pa, pb) : a.getKey().compareTo(b.getKey());
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