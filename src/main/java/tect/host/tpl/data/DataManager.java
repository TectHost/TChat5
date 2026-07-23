package tect.host.tpl.data;

import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigManager;
import tect.host.tpl.data.storage.StorageProvider;
import tect.host.tpl.data.storage.StorageProviderFactory;
import tect.host.tpl.util.logging.DebugLogger;
import tect.host.tpl.util.Utils;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;

/**
 * Data access orchestrator that manages storage initialization, migrations, and connections
 *.
 * Storage-specific logic is handled by the resolved StorageProvider
 */
public final class DataManager {

    private static final String SELECT_MAX_VERSION = "SELECT COALESCE(MAX(version), 0) FROM schema_migrations WHERE module = ?";

    private static final String INSERT_MIGRATION = "INSERT INTO schema_migrations (module, version, applied_at) VALUES (?, ?, ?)";

    private final StorageProvider storageProvider;
    private final DataSource dataSource;
    private final Logger logger;
    private final DebugLogger debugLogger;

    public DataManager(@NonNull File dataFolder, @NonNull ConfigManager config, @NonNull Logger logger, @NonNull DebugLogger debugLogger) {
        this.logger = logger;
        this.debugLogger = debugLogger;

        DataMethod method = DataMethod.resolve(config);
        this.storageProvider = StorageProviderFactory.create(method, dataFolder, config, getClass().getClassLoader(), debugLogger);

        debugLogger.info("Preparing storage (%s), first run may take a few seconds while dependencies download".formatted(method));
        storageProvider.prepare();
        this.dataSource = storageProvider.buildPool();
        ensureMigrationsTable();

        debugLogger.info("DataManager initialized with method %s".formatted(storageProvider.method()));
    }

    @NonNull
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @NonNull
    public DataMethod getMethod() {
        return storageProvider.method();
    }

    /** run migrations */
    public void initializeRepository(@NonNull Repository repository) {
        String moduleId = repository.getModuleId();
        List<String> migrations = repository.getMigrations(getMethod());

        if (migrations.isEmpty()) {
            repository.onInitialized();
            return;
        }

        try (Connection conn = getConnection()) {
            int currentVersion = queryCurrentVersion(conn, moduleId);
            int pending = migrations.size() - currentVersion;

            if (pending <= 0) {
                debugLogger.info("[DataManager] %s, schema up to date (v%d)".formatted(moduleId, currentVersion));
                repository.onInitialized();
                return;
            }

            conn.setAutoCommit(false);
            try {
                for (int i = currentVersion; i < migrations.size(); i++) {
                    int newVersion = i + 1;
                    Utils.log(logger, "INFO", "[DataManager] %s, applying migration v%d".formatted(moduleId, newVersion));

                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(migrations.get(i));
                    }
                    try (PreparedStatement ps = conn.prepareStatement(INSERT_MIGRATION)) {
                        ps.setString(1, moduleId);
                        ps.setInt(2, newVersion);
                        ps.setLong(3, Instant.now().getEpochSecond());
                        ps.executeUpdate();
                    }
                }
                conn.commit();
                Utils.log(logger, "INFO", "[DataManager] %s, migrated to v%d".formatted(moduleId, migrations.size()));
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            Utils.log(logger, "SEVERE", "[DataManager] Migration failed for module '%s': %s".formatted(moduleId, e.getMessage()));
            throw new RuntimeException("Schema migration failed for module: " + moduleId, e);
        }

        repository.onInitialized();
    }

    public void close() {
        if (dataSource instanceof Closeable closeable) {
            try {
                closeable.close();
            } catch (IOException e) {
                Utils.log(logger, "WARNING", "Error closing data source: " + e.getMessage());
            }
        }
        storageProvider.shutdown();
        debugLogger.info("DataManager pool closed.");
    }

    private void ensureMigrationsTable() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(storageProvider.migrationsTableDdl());
        } catch (SQLException e) {
            Utils.log(logger, "SEVERE", "Failed to create schema_migrations table: " + e.getMessage());
            throw new RuntimeException("Cannot initialize DataManager", e);
        }
    }

    private int queryCurrentVersion(@NonNull Connection conn, @NonNull String moduleId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_MAX_VERSION)) {
            ps.setString(1, moduleId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}