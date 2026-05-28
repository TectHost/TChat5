package tect.host.tpl.module;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.TChat;
import tect.host.tpl.config.ConfigFile;
import tect.host.tpl.config.ConfigManager;
import tect.host.tpl.config.MessagesManager;
import tect.host.tpl.data.DataManager;
import tect.host.tpl.module.hook.placeholderapi.PlaceholderApiHook;
import tect.host.tpl.module.registry.ModuleManager;

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
    private final DataManager dataManager;
    private @Nullable ModuleManager moduleManager;

    public ModuleContext(@NonNull TChat plugin, @NonNull ConfigManager coreConfig, @NonNull MessagesManager messagesManager, @NonNull PlaceholderApiHook placeholderApiHook, @NonNull SchedulerAccess scheduler, @NonNull DataManager dataManager) {
        this.plugin = plugin;
        this.coreConfig = coreConfig;
        this.messagesManager = messagesManager;
        this.placeholderApiHook = placeholderApiHook;
        this.scheduler = scheduler;
        this.pluginVersion = plugin.getPluginMeta().getVersion();
        this.logger = plugin.getLogger();
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
    public @NonNull DataManager getDataManager() { return dataManager; }

    public @NonNull ModuleManager getModuleManager() {
        if (moduleManager == null) throw new IllegalStateException("ModuleManager not yet initialized");
        return moduleManager;
    }

    public @NonNull Collection<? extends Player> getOnlinePlayers() {
        return plugin.getServer().getOnlinePlayers();
    }
}