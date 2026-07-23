package tect.host.tpl.data.storage.sqlite;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tect.host.tpl.data.DataMethod;
import tect.host.tpl.dependency.Dependency;
import tect.host.tpl.dependency.DependencyManager;
import tect.host.tpl.data.storage.DriverShim;
import tect.host.tpl.data.storage.HikariPoolFactory;
import tect.host.tpl.data.storage.StorageProvider;
import tect.host.tpl.util.logging.DebugLogger;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public final class SqliteStorageProvider implements StorageProvider {

    private static final String CREATE_MIGRATIONS_TABLE = """
            CREATE TABLE IF NOT EXISTS schema_migrations (
                module TEXT NOT NULL,
                version INTEGER NOT NULL,
                applied_at INTEGER NOT NULL,
                PRIMARY KEY (module, version)
            )
            """;

    /** Binary name of the SQLite JDBC driver */
    private static final String DRIVER_CLASS_NAME = "org.sqlite.JDBC";

    private static final long DEPENDENCY_LOAD_TIMEOUT_SECONDS = 30;

    private final File dataFolder;
    private final DependencyManager dependencyManager;
    private final DebugLogger debugLogger;

    private volatile @Nullable DriverShim registeredDriver;

    public SqliteStorageProvider(@NonNull File dataFolder, @NonNull ClassLoader parentClassLoader, @NonNull DebugLogger debugLogger) {
        this.dataFolder = dataFolder;
        this.debugLogger = debugLogger;
        this.dependencyManager = new DependencyManager(dataFolder, parentClassLoader, debugLogger);
    }

    @Override
    public @NonNull DataMethod method() {
        return DataMethod.SQLITE;
    }

    @Override
    public void prepare() {
        loadDependencies();
        registerDriver();
    }

    @Override
    public @NonNull DataSource buildPool() {
        File dbFolder = new File(dataFolder, "data");
        dbFolder.mkdirs();
        File dbFile = new File(dbFolder, "tchat.db");

        Properties props = new Properties();
        props.setProperty("poolName", "TChat-HikariPool-SQLite");
        props.setProperty("jdbcUrl", "jdbc:sqlite:%s".formatted(dbFile.getAbsolutePath()));
        props.setProperty("maximumPoolSize", "1");
        props.setProperty("minimumIdle", "1");
        props.setProperty("connectionTimeout", "5000");

        props.setProperty("dataSource.journal_mode", "WAL");
        props.setProperty("dataSource.synchronous", "NORMAL");
        props.setProperty("dataSource.foreign_keys", "ON");
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
        DriverShim shim = registeredDriver;
        if (shim != null) {
            try {
                DriverManager.deregisterDriver(shim);
            } catch (SQLException e) {
                debugLogger.warn("Failed to deregister SQLite driver: " + e.getMessage());
            }
            registeredDriver = null;
        }
        dependencyManager.shutdown();
    }

    private void loadDependencies() {
        try {
            dependencyManager.loadAll(EnumSet.of(Dependency.SQLITE_JDBC, Dependency.HIKARICP))
                    .orTimeout(DEPENDENCY_LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .join();
        } catch (Exception e) {
            throw new RuntimeException("The SQLite databases could not be prepared", e);
        }
    }

    /**
     * Registers the SQLite driver through a shim because it is loaded from an
     * isolated class loader
     */
    private void registerDriver() {
        try {
            Class<?> driverClass = dependencyManager.getClassLoader().loadClass(DRIVER_CLASS_NAME);
            Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
            DriverShim shim = new DriverShim(driver);
            DriverManager.registerDriver(shim);
            registeredDriver = shim;
        } catch (ReflectiveOperationException | SQLException e) {
            throw new RuntimeException("The SQLite driver could not be registered", e);
        }
    }
}