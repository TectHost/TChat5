package tect.host.tpl;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import tect.host.tpl.command.TChatCommand;
import tect.host.tpl.config.ConfigManager;
import tect.host.tpl.config.MessagesManager;
import tect.host.tpl.listener.ChatListener;
import tect.host.tpl.listener.PlayerJoinListener;
import tect.host.tpl.manager.*;
import tect.host.tpl.module.BukkitSchedulerAccess;
import tect.host.tpl.module.ModuleCommand;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.SchedulerAccess;
import tect.host.tpl.module.hook.placeholderapi.PlaceholderApiHook;

import java.util.List;

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

        getServer().getPluginManager().registerEvents(new ChatListener(new ChatProcessor(moduleManager)), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(new JoinProcessor(moduleManager)), this);

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
            TChatCommand tChatCommand = new TChatCommand(this);
            event.registrar().register("tchat", tChatCommand);
            event.registrar().register("chat",  tChatCommand);

            for (ModuleDescriptor descriptor : ModuleRegistry.createDefaultRegistry()) {
                var cmdFactory = descriptor.getCommandFactory();
                if (cmdFactory != null) {
                    ModuleCommand cmd = cmdFactory.apply(moduleManager);
                    event.registrar().register(cmd.getName(), cmd);
                    for (String alias : cmd.getAliases()) {
                        event.registrar().register(alias, cmd);
                    }
                }
            }
        });
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }
}