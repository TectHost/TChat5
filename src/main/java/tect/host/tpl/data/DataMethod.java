package tect.host.tpl.data;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigManager;

public enum DataMethod {
    SQLITE,
    MARIADB;

    public static @NonNull DataMethod resolve(@NonNull ConfigManager config) {
        String method = config.getString("storage.method");
        if (method.isBlank()) return SQLITE;

        try {
            return DataMethod.valueOf(method.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return SQLITE;
        }
    }
}