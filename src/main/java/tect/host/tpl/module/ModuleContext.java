package tect.host.tpl.module;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.TChat;
import tect.host.tpl.action.ActionExecutor;
import tect.host.tpl.config.ConfigFile;
import tect.host.tpl.config.ConfigManager;
import tect.host.tpl.config.MessagesManager;
import tect.host.tpl.data.DataManager;
import tect.host.tpl.module.hook.placeholderapi.PlaceholderApiHook;
import tect.host.tpl.module.registry.ModuleManager;
import tect.host.tpl.pipeline.ChatProcessor;
import tect.host.tpl.util.Utils;
import tect.host.tpl.util.logging.DebugLogger;

import java.io.File;
import java.util.Collection;
import java.util.logging.Logger;

public final class ModuleContext {

    private final TChat plugin;
    private final ConfigManager coreConfig;
    private final MessagesManager messagesManager;
    private final PlaceholderApiHook placeholderApiHook;
    private final SchedulerAccess scheduler;
    private final String pluginVersion;
    private final Logger logger;
    private final DebugLogger debugLogger;
    private final DataManager dataManager;
    private @Nullable ModuleManager moduleManager;
    private volatile @Nullable ActionExecutor actionExecutor;
    private volatile @Nullable ChatProcessor chatProcessor;

    public ModuleContext(@NonNull TChat plugin, @NonNull ConfigManager coreConfig, @NonNull MessagesManager messagesManager, @NonNull PlaceholderApiHook placeholderApiHook, @NonNull SchedulerAccess scheduler, @NonNull DebugLogger debugLogger, @NonNull DataManager dataManager) {
        this.plugin = plugin;
        this.coreConfig = coreConfig;
        this.messagesManager = messagesManager;
        this.placeholderApiHook = placeholderApiHook;
        this.scheduler = scheduler;
        this.pluginVersion = Utils.getPluginVersion(plugin);
        this.logger = plugin.getLogger();
        this.debugLogger = debugLogger;
        this.dataManager = dataManager;
    }

    public @NonNull ConfigFile createConfigFile(@NonNull String fileName, @Nullable String subFolder) {
        return new ConfigFile(plugin, fileName, subFolder);
    }

    public void setModuleManager(@NonNull ModuleManager moduleManager) {
        if (this.moduleManager != null) throw new IllegalStateException("ModuleManager already set");
        this.moduleManager = moduleManager;
    }

    public @NonNull ConfigManager getCoreConfig() { return coreConfig; }
    public @NonNull MessagesManager getMessagesManager() { return messagesManager; }
    public @NonNull PlaceholderApiHook getPlaceholderApiHook() { return placeholderApiHook; }
    public @NonNull SchedulerAccess getScheduler() { return scheduler; }
    public @NonNull String getPluginVersion() { return pluginVersion; }
    public @NonNull Logger getLogger() { return logger; }
    public @NonNull DebugLogger getDebugLogger() { return debugLogger; }
    public @NonNull DataManager getDataManager() { return dataManager; }

    public @NonNull ModuleManager getModuleManager() {
        if (moduleManager == null) throw new IllegalStateException("ModuleManager not yet initialized");
        return moduleManager;
    }

    public @NonNull ActionExecutor getActionExecutor() {
        ActionExecutor local = actionExecutor;
        if (local == null) {
            synchronized (this) {
                local = actionExecutor;
                if (local == null) {
                    local = new ActionExecutor(this);
                    actionExecutor = local;
                }
            }
        }
        return local;
    }

    public @NonNull ChatProcessor getChatProcessor() {
        ChatProcessor local = chatProcessor;
        if (local == null) {
            synchronized (this) {
                local = chatProcessor;
                if (local == null) {
                    local = new ChatProcessor(getModuleManager());
                    chatProcessor = local;
                }
            }
        }
        return local;
    }

    public @NonNull Collection<? extends Player> getOnlinePlayers() {
        return plugin.getServer().getOnlinePlayers();
    }

    @Contract(pure = true)
    public @NonNull File getPluginDataFolder() {
        return plugin.getDataFolder();
    }
}