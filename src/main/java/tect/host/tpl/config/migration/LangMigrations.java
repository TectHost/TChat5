package tect.host.tpl.config.migration;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.logging.Logger;

public final class LangMigrations {

    private LangMigrations() {}

    @Contract("_, _ -> new")
    public static @NonNull ConfigMigrator create(@NonNull Logger logger, String langFileName) {
        return new ConfigMigrator(logger, langFileName);

            // v0 -> v1
            //.addMigration(config -> {
            //    if (!config.isSet("path.path")) {
            //        config.set("path.path", "message");
            //    }
            //});
    }
}