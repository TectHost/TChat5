package tect.host.tpl.data.storage;

import org.jspecify.annotations.NonNull;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.util.Properties;

/**
 * Creates HikariCP data sources loaded from an isolated class loader
 *.
 * Uses reflection because HikariCP is provided at runtime instead of being
 * included in the plugin classpath
 */
public final class HikariPoolFactory {

    private static final String CONFIG_CLASS = "com.zaxxer.hikari.HikariConfig";
    private static final String DATASOURCE_CLASS = "com.zaxxer.hikari.HikariDataSource";

    private HikariPoolFactory() {
    }

    public static @NonNull DataSource create(@NonNull ClassLoader classLoader, @NonNull Properties properties) {
        try {
            Class<?> configClass = classLoader.loadClass(CONFIG_CLASS);
            Constructor<?> configConstructor = configClass.getConstructor(Properties.class);
            Object config = configConstructor.newInstance(properties);

            Class<?> dataSourceClass = classLoader.loadClass(DATASOURCE_CLASS);
            Constructor<?> dataSourceConstructor = dataSourceClass.getConstructor(configClass);

            return (DataSource) dataSourceConstructor.newInstance(config);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("The HikariCP pool could not be built", e);
        }
    }
}