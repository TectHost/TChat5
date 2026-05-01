package tect.host.tpl.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.TChat;
import tect.host.tpl.config.migration.ConfigMigrator;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class ConfigFile {

    private final TChat plugin;
    private final String fileName;
    private final String folderName;

    private File file;
    private FileConfiguration fileConfiguration;
    private @Nullable ConfigMigrator migrator;

    public ConfigFile(TChat plugin, String fileName, String folderName) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.folderName = folderName;
    }

    public ConfigFile(TChat plugin, String fileName) {
        this(plugin, fileName, null);
    }

    public void setMigrator(@NonNull ConfigMigrator migrator) {
        this.migrator = migrator;
    }

    public void register() {
        file = resolveFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            plugin.getLogger().log(Level.SEVERE, "Could not create config directory for " + fileName);
        }

        if (!file.exists()) {
            String resourcePath = folderName != null
                    // If a folder is specified, build: folderName/fileName (if it exists in resources)
                    ? folderName + "/" + fileName
                    // Otherwise, just use the file name at root level
                    : fileName;

            try (var in = plugin.getResource(resourcePath)) {
                if (in == null) {
                    plugin.getLogger().warning("Resource not found: " + resourcePath);
                } else {
                    file.getParentFile().mkdirs();
                    java.nio.file.Files.copy(in, file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed copying resource: " + resourcePath, e);
            }
        }

        reload();

        if (migrator != null) {
            boolean migrated = migrator.migrate(fileConfiguration, file);
            if (migrated) reload();
        }
    }

    public void save() {
        if (fileConfiguration == null || file == null) return;
        try {
            fileConfiguration.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config: " + fileName, e);
        }
    }

    public void reload() {
        if (file == null) file = resolveFile();

        fileConfiguration = new YamlConfiguration();
        try {
            fileConfiguration.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load config: " + fileName, e);
        }
    }

    public FileConfiguration get() {
        if (fileConfiguration == null) reload();
        return fileConfiguration;
    }

    public File getFile() {
        return file;
    }

    public String getFileName() {
        return fileName;
    }

    private @NonNull File resolveFile() {
        return folderName != null
                // If a folder is specified, build: /pluginData/folderName/fileName
                ? new File(plugin.getDataFolder() + File.separator + folderName, fileName)
                // Without subdirectory (plugin root)
                : new File(plugin.getDataFolder(), fileName);
    }
}