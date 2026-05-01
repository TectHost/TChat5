package tect.host.tpl.config.migration;

import org.bukkit.configuration.file.FileConfiguration;

@FunctionalInterface
public interface ConfigMigration {
    void apply(FileConfiguration config);
}