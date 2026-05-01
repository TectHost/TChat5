package tect.host.tpl;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import tect.host.tpl.command.TChatCommand;
import tect.host.tpl.config.ConfigManager;
import tect.host.tpl.config.MessagesManager;
import tect.host.tpl.listener.ChatListener;
import tect.host.tpl.listener.PlayerJoinListener;
import tect.host.tpl.manager.ChatProcessor;
import tect.host.tpl.manager.JoinProcessor;
import tect.host.tpl.manager.ModuleManager;
import tect.host.tpl.manager.ModuleRegistry;
import tect.host.tpl.module.BukkitSchedulerAccess;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.SchedulerAccess;
import tect.host.tpl.module.hook.placeholderapi.PlaceholderApiHook;

public final class TChat extends JavaPlugin {

    private ConfigManager configManager;
    private MessagesManager messagesManager;
    private PlaceholderApiHook placeholderApiHook;
    private ModuleManager moduleManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);

        placeholderApiHook = new PlaceholderApiHook();

        messagesManager = new MessagesManager(this, configManager, placeholderApiHook);

        SchedulerAccess scheduler = new BukkitSchedulerAccess(this);

        ModuleContext moduleContext = new ModuleContext(this, configManager, messagesManager, placeholderApiHook, scheduler);

        moduleManager = new ModuleManager(getLogger(), configManager, moduleContext);
        moduleManager.registerDescriptors(ModuleRegistry.createDefaultRegistry());
        moduleManager.loadEnabledModules();

        placeholderApiHook.registerExpansion(this, moduleManager);

        ChatProcessor chatProcessor = new ChatProcessor(moduleManager);
        getServer().getPluginManager().registerEvents(new ChatListener(chatProcessor), this);

        JoinProcessor joinProcessor = new JoinProcessor(moduleManager);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(joinProcessor), this);

        registerCommands();
        new Metrics(this, 23305);
    }

    @Override
    public void onDisable() {
        if (placeholderApiHook != null) placeholderApiHook.unregisterExpansion();
        if (moduleManager != null) moduleManager.unloadAll();
    }

    public void reloadPluginState() {
        configManager.reload();
        messagesManager.reload();
        moduleManager.reloadModules();
    }

    private void registerCommands() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            TChatCommand cmd = new TChatCommand(this);

            event.registrar().register("tchat", cmd);
            event.registrar().register("chat", cmd);
        });
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }
}