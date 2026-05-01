package tect.host.tpl.config.migration;

import org.jspecify.annotations.NonNull;

import java.util.logging.Logger;

public final class ConfigMigrations {

    private ConfigMigrations() {}

    public static @NonNull ConfigMigrator forConfig(@NonNull Logger logger) {
        return new ConfigMigrator(logger, "config.yml");

            // v0 -> v1
            //.addMigration(config -> {
            //  new options here
            //  if (!config.isSet("modules.announcements")) {
            //    config.set("modules.announcements", false);
            //  }
            //});
    }
}