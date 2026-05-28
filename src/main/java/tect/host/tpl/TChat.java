package tect.host.tpl;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import tect.host.tpl.command.TChatCommand;
import tect.host.tpl.config.ConfigManager;
import tect.host.tpl.config.MessagesManager;
import tect.host.tpl.data.DataManager;
import tect.host.tpl.listener.PlayerChatListener;
import tect.host.tpl.listener.PlayerCommandListener;
import tect.host.tpl.listener.PlayerJoinListener;
import tect.host.tpl.listener.PlayerQuitListener;
import tect.host.tpl.module.BukkitSchedulerAccess;
import tect.host.tpl.module.ModuleCommand;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.SchedulerAccess;
import tect.host.tpl.module.hook.placeholderapi.PlaceholderApiHook;
import tect.host.tpl.module.registry.ModuleManager;
import tect.host.tpl.module.registry.ModuleRegistry;
import tect.host.tpl.pipeline.ChatProcessor;
import tect.host.tpl.pipeline.CommandProcessor;
import tect.host.tpl.pipeline.JoinProcessor;
import tect.host.tpl.pipeline.QuitProcessor;

public final class TChat extends JavaPlugin {

    private ConfigManager configManager;
    private MessagesManager messagesManager;
    private PlaceholderApiHook placeholderApiHook;
    private ModuleManager moduleManager;
    private DataManager dataManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);

        placeholderApiHook = new PlaceholderApiHook();

        messagesManager = new MessagesManager(this, configManager, placeholderApiHook);

        dataManager = new DataManager(getDataFolder(), configManager, getLogger());

        final SchedulerAccess scheduler = new BukkitSchedulerAccess(this);

        final ModuleContext moduleContext = new ModuleContext(this, configManager, messagesManager, placeholderApiHook, scheduler, dataManager);

        moduleManager = new ModuleManager(getLogger(), configManager, moduleContext);
        moduleContext.setModuleManager(moduleManager);
        moduleManager.registerDescriptors(ModuleRegistry.createDefaultRegistry());
        moduleManager.loadEnabledModules();

        placeholderApiHook.registerExpansion(this, moduleManager);

        getServer().getPluginManager().registerEvents(new PlayerChatListener(new ChatProcessor(moduleManager)), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(new JoinProcessor(moduleManager)), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(new QuitProcessor(moduleManager)), this);
        getServer().getPluginManager().registerEvents(new PlayerCommandListener(new CommandProcessor(moduleManager)), this);

        registerCommands();
        new Metrics(this, 23305);
    }

    @Override
    public void onDisable() {
        if (placeholderApiHook != null) placeholderApiHook.unregisterExpansion();
        if (moduleManager != null) moduleManager.unloadAll();
        if (dataManager != null) dataManager.close();
    }

    public void reloadPluginState() {
        configManager.reload();
        messagesManager.reload();
        moduleManager.reloadModules();
    }

    private void registerCommands() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final TChatCommand tChatCommand = new TChatCommand(this);
            event.registrar().register("tchat", tChatCommand);
            event.registrar().register("chat", tChatCommand);

            for (ModuleCommand cmd : moduleManager.getActiveCommands()) {
                event.registrar().register(cmd.getName(), cmd);
                for (String alias : cmd.getAliases()) {
                    event.registrar().register(alias, cmd);
                }
            }
        });
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }
}