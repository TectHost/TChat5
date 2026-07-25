package tect.host.tpl;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import tect.host.tpl.command.TChatCommand;
import tect.host.tpl.config.ConfigManager;
import tect.host.tpl.config.MessagesManager;
import tect.host.tpl.data.DataManager;
import tect.host.tpl.listener.*;
import tect.host.tpl.module.BukkitSchedulerAccess;
import tect.host.tpl.module.ModuleCommand;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.SchedulerAccess;
import tect.host.tpl.module.hook.placeholderapi.PlaceholderApiHook;
import tect.host.tpl.module.impl.command.customcommands.CustomCommandExecutor;
import tect.host.tpl.module.impl.command.customcommands.CustomCommandsModule;
import tect.host.tpl.module.registry.ModuleManager;
import tect.host.tpl.module.registry.ModuleRegistry;
import tect.host.tpl.pipeline.CommandProcessor;
import tect.host.tpl.pipeline.JoinProcessor;
import tect.host.tpl.pipeline.QuitProcessor;
import tect.host.tpl.util.logging.DebugLogger;
import tect.host.tpl.util.logging.StartupLogger;
import tect.host.tpl.util.logging.StartupMode;

public final class TChat extends JavaPlugin {

    private ConfigManager configManager;
    private MessagesManager messagesManager;
    private PlaceholderApiHook placeholderApiHook;
    private ModuleManager moduleManager;
    private DataManager dataManager;
    private DebugLogger debugLogger;

    private int[] lastCommandCounts = {0, 0};

    @Override
    public void onEnable() {
        final long startTime = System.currentTimeMillis();

        configManager = new ConfigManager(this);
        debugLogger = DebugLogger.of(getLogger(), configManager);
        placeholderApiHook = new PlaceholderApiHook();
        messagesManager = new MessagesManager(this, configManager, placeholderApiHook);
        dataManager = new DataManager(getDataFolder(), configManager, getLogger()   , debugLogger);

        setupModules();
        registerListeners();
        registerCommands(startTime);

        new Metrics(this, 23305);
    }

    @Override
    public void onDisable() {
        if (placeholderApiHook != null) placeholderApiHook.unregisterExpansion();
        if (moduleManager != null) moduleManager.unloadAll();
        if (dataManager != null) dataManager.close();
    }

    public void reloadPluginState() {
        final long startTime = System.currentTimeMillis();

        configManager.reload();
        debugLogger.refresh();
        messagesManager.reload();
        moduleManager.reloadModules();
        placeholderApiHook.registerExpansion(this, moduleManager);

        printBanner(StartupMode.RELOAD, startTime);
    }

    private void setupModules() {
        final SchedulerAccess scheduler = new BukkitSchedulerAccess(this);
        final ModuleContext moduleContext = new ModuleContext(this, configManager, messagesManager, placeholderApiHook, scheduler, debugLogger, dataManager);

        moduleManager = new ModuleManager(debugLogger, configManager, moduleContext);
        moduleContext.setModuleManager(moduleManager);
        moduleManager.registerDescriptors(ModuleRegistry.createDefaultRegistry());
        moduleManager.loadEnabledModules();

        placeholderApiHook.registerExpansion(this, moduleManager);
    }

    private void registerListeners() {
        final var pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerChatListener(moduleManager.getModuleContext().getChatProcessor()), this);
        pm.registerEvents(new PlayerJoinListener(new JoinProcessor(moduleManager)), this);
        pm.registerEvents(new PlayerQuitListener(new QuitProcessor(moduleManager)), this);
        pm.registerEvents(new PlayerCommandListener(new CommandProcessor(moduleManager)), this);
        pm.registerEvents(new MenuClickListener(), this);
    }

    private void registerCommands(long startTime) {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final int[] counts = {0, 0};

            final TChatCommand tChatCommand = new TChatCommand(this);
            tryRegister(event.registrar(), "tchat", tChatCommand, counts);
            tryRegister(event.registrar(), "chat", tChatCommand, counts);

            for (ModuleCommand cmd : moduleManager.getActiveCommands()) {
                tryRegister(event.registrar(), cmd.getName(), cmd, counts);
                for (String alias : cmd.getAliases()) {
                    tryRegister(event.registrar(), alias, cmd, counts);
                }
            }

            CustomCommandsModule ccm = moduleManager.getModule("custom-commands", CustomCommandsModule.class);
            if (ccm != null) {
                for (CustomCommandExecutor executor : ccm.getExecutors()) {
                    tryRegister(event.registrar(), executor.getName(), executor, counts);
                    for (String alias : executor.getAliases()) {
                        tryRegister(event.registrar(), alias, executor, counts);
                    }
                }
            }

            lastCommandCounts = counts;
            printBanner(StartupMode.STARTUP, startTime);
        });
    }

    private <T extends BasicCommand> void tryRegister(Commands registrar, String name, T command, int[] counts) {
        try {
            registrar.register(name, command);
            counts[0]++;
        } catch (Exception e) {
            getLogger().warning("Failed to register command '%s': %s".formatted(name, e.getMessage()));
            counts[1]++;
        }
    }

    private void printBanner(StartupMode mode, long startTime) {
        final ModuleManager.LoadResult modules = moduleManager.getLoadResult();
        final ModuleManager.MenuLoadResult menus = moduleManager.getMenuLoadResult();
        final ModuleManager.ModuleTimings timings = moduleManager.getModuleTimings();

        final boolean papiActive = placeholderApiHook.isAvailable();

        final StartupLogger.Stats stats = new StartupLogger.Stats(
                modules.loaded(),
                modules.skipped(),
                modules.failed(),
                menus.enabled(),
                menus.disabled(),
                menus.skipped(),
                lastCommandCounts[0],
                lastCommandCounts[1],
                papiActive ? 1 : 0,
                papiActive ? 0 : 1,
                System.currentTimeMillis() - startTime,
                debugLogger.isEnabled(),
                timings.dependencyResolutionMs(),
                timings.totalLoadMs(),
                timings.slowestModuleId(),
                timings.slowestModuleMs()
        );

        new StartupLogger(this).print(stats, mode);
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }
}