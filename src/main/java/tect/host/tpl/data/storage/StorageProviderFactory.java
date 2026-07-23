package tect.host.tpl.data.storage;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigManager;
import tect.host.tpl.data.DataMethod;
import tect.host.tpl.data.storage.mariadb.MariaDbStorageProvider;
import tect.host.tpl.data.storage.sqlite.SqliteStorageProvider;
import tect.host.tpl.util.logging.DebugLogger;

import java.io.File;

/**
 * Factory responsible for creating the correct StorageProvider for each
 * DataMethod
 *.
 * Adding a new storage backend only requires a new provider implementation and
 * a new mapping here
 */
public final class StorageProviderFactory {

    private StorageProviderFactory() {}

    public static @NonNull StorageProvider create(@NonNull DataMethod method, @NonNull File dataFolder, @NonNull ConfigManager config, @NonNull ClassLoader parentClassLoader, @NonNull DebugLogger debugLogger) {
        return switch (method) {
            case SQLITE -> new SqliteStorageProvider(dataFolder, parentClassLoader, debugLogger);
            case MARIADB -> new MariaDbStorageProvider(dataFolder, config, parentClassLoader, debugLogger);
        };
    }
}