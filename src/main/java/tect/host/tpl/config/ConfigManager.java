package tect.host.tpl.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.TChat;
import tect.host.tpl.config.migration.ConfigMigrations;

public class ConfigManager {

    private final ConfigFile configFile;
    private FileConfiguration config;

    public ConfigManager(TChat plugin) {
        this.configFile = new ConfigFile(plugin, "config.yml");
        this.configFile.setMigrator(ConfigMigrations.forConfig(plugin.getLogger()));
        this.configFile.register();
        this.config = configFile.get();
    }

    public void reload() {
        configFile.reload();
        config = configFile.get();
    }

    public @NonNull FileConfiguration getConfig() {
        return config;
    }

    public boolean isModuleEnabled(@NonNull String moduleId) {
        return config.getBoolean("modules." + moduleId, false);
    }

    public boolean getBoolean(@NonNull String path) {
        return config.getBoolean(path);
    }

    public @NonNull String getString(@NonNull String path) {
        return config.getString(path, "");
    }

    public int getInt(@NonNull String path) {
        return config.getInt(path);
    }

    public double getDouble(@NonNull String path) {
        return config.getDouble(path);
    }
}
