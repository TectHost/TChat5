package tect.host.tpl.data.storage.mariadb;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigManager;
import tect.host.tpl.data.DataMethod;
import tect.host.tpl.dependency.Dependency;
import tect.host.tpl.dependency.DependencyManager;
import tect.host.tpl.data.storage.DriverShim;
import tect.host.tpl.data.storage.HikariPoolFactory;
import tect.host.tpl.data.storage.StorageProvider;
import tect.host.tpl.util.logging.DebugLogger;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumSet;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class MariaDbStorageProvider implements StorageProvider {

    private static final String CREATE_MIGRATIONS_TABLE = """
            CREATE TABLE IF NOT EXISTS schema_migrations (
                module VARCHAR(191) NOT NULL,
                version INT NOT NULL,
                applied_at BIGINT NOT NULL,
                PRIMARY KEY (module, version)
            )
            """;

    /** Binary name of the MariaDB JDBC driver */
    private static final String DRIVER_CLASS_NAME = "org.mariadb.jdbc.Driver";

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[A-Za-z0-9_]+$");
    private static final long DEPENDENCY_LOAD_TIMEOUT_SECONDS = 30;

    private final DebugLogger debugLogger;
    private final DependencyManager dependencyManager;

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean useSsl;
    private final int maxPoolSize;
    private final int minIdle;

    public MariaDbStorageProvider(@NonNull File dataFolder, @NonNull ConfigManager config, @NonNull ClassLoader parentClassLoader, @NonNull DebugLogger debugLogger) {
        this.debugLogger = debugLogger;
        this.dependencyManager = new DependencyManager(dataFolder, parentClassLoader, debugLogger);

        String host = config.getString("storage.remote.host");
        this.host = host.isBlank() ? "localhost" : host;

        int port = config.getInt("storage.remote.port");
        this.port = port > 0 ? port : 3306;

        String database = config.getString("storage.remote.database");
        this.database = database.isBlank() ? "tchat" : database;

        if (!SAFE_IDENTIFIER.matcher(this.database).matches()) {
            throw new IllegalArgumentException("Invalid storage.remote.database: '%s' (letters, numbers, and underscores only)".formatted(this.database));
        }

        String username = config.getString("storage.remote.username");
        this.username = username.isBlank() ? "root" : username;

        this.password = config.getString("storage.remote.password");
        this.useSsl = config.getBoolean("storage.remote.useSSL");

        int maxPoolSize = config.getInt("storage.remote.pool.maximumSize");
        this.maxPoolSize = maxPoolSize > 0 ? maxPoolSize : 10;

        int minIdle = config.getInt("storage.remote.pool.minimumIdle");
        this.minIdle = minIdle > 0 ? minIdle : 2;
    }

    @Override
    public @NonNull DataMethod method() {
        return DataMethod.MARIADB;
    }

    @Override
    public void prepare() {
        loadDependencies();
        registerDriver();
        ensureDatabaseExists();
    }

    @Override
    public @NonNull DataSource buildPool() {
        Properties props = new Properties();
        props.setProperty("poolName", "TChat-HikariPool-MariaDB");
        props.setProperty("jdbcUrl", databaseUrl());
        props.setProperty("username", username);
        props.setProperty("password", password);
        props.setProperty("maximumPoolSize", String.valueOf(maxPoolSize));
        props.setProperty("minimumIdle", String.valueOf(minIdle));
        props.setProperty("connectionTimeout", "10000");
        props.setProperty("dataSource.cachePrepStmts", "true");
        props.setProperty("dataSource.prepStmtCacheSize", "64");

        return HikariPoolFactory.create(dependencyManager.getClassLoader(), props);
    }

    @Override
    public @NonNull String migrationsTableDdl() {
        return CREATE_MIGRATIONS_TABLE;
    }

    @Override
    public void shutdown() {
        dependencyManager.shutdown();
    }

    private void loadDependencies() {
        try {
            dependencyManager.loadAll(EnumSet.of(Dependency.MARIADB_JAVA_CLIENT, Dependency.HIKARICP))
                    .orTimeout(DEPENDENCY_LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .join();
        } catch (Exception e) {
            throw new RuntimeException("The MariaDB instances could not be set up", e);
        }
    }

    private void registerDriver() {
        try {
            Class<?> driverClass = dependencyManager.getClassLoader().loadClass(DRIVER_CLASS_NAME);
            Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
            DriverManager.registerDriver(new DriverShim(driver));
        } catch (ReflectiveOperationException | SQLException e) {
            throw new RuntimeException("The MariaDB driver could not be registered", e);
        }
    }

    /** Creates the configured database if it does not exist */
    private void ensureDatabaseExists() {
        try (Connection conn = DriverManager.getConnection(serverUrl(), username, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE DATABASE IF NOT EXISTS `%s` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci".formatted(database));
            debugLogger.info("[MariaDbStorageProvider] Database '%s' verified/created".formatted(database));
        } catch (SQLException e) {
            throw new RuntimeException("The '%s' database could not be verified/created".formatted(database), e);
        }
    }

    private @NonNull String serverUrl() {
        return "jdbc:mariadb://%s:%d/?%s".formatted(host, port, connectionOptions());
    }

    private @NonNull String databaseUrl() {
        return "jdbc:mariadb://%s:%d/%s?%s".formatted(host, port, database, connectionOptions());
    }

    @Contract(pure = true)
    private @NonNull String connectionOptions() {
        return "useSSL=%b&restrictedAuth=mysql_native_password,caching_sha2_password".formatted(useSsl);
    }
}