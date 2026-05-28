package tect.host.tpl.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigManager;

import java.io.File;
import java.sql.*;
import java.util.List;
import java.util.logging.Logger;

public final class DataManager {

    private static final String CREATE_MIGRATIONS_TABLE = """
            CREATE TABLE IF NOT EXISTS schema_migrations (
                module TEXT NOT NULL,
                version INTEGER NOT NULL,
                applied_at INTEGER NOT NULL DEFAULT (unixepoch()),
                PRIMARY KEY (module, version)
            )
            """;

    private static final String SELECT_MAX_VERSION = "SELECT COALESCE(MAX(version), 0) FROM schema_migrations WHERE module = ?";

    private static final String INSERT_MIGRATION = "INSERT INTO schema_migrations (module, version) VALUES (?, ?)";

    private final HikariDataSource dataSource;
    private final Method method;
    private final Logger logger;

    public DataManager(@NonNull File dataFolder, @NonNull ConfigManager config, @NonNull Logger logger) {
        this.logger = logger;
        this.method = resolveMethod(config);
        this.dataSource = buildPool(dataFolder, config);
        ensureMigrationsTable();
        logger.info("DataManager initialized with method %s".formatted(method));
    }

    @NonNull
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @NonNull
    public Method getMethod() { return method; }

    /** run migrations */
    public void initializeRepository(@NonNull Repository repository) {
        String moduleId = repository.getModuleId();
        List<String> migrations = repository.getMigrations();

        if (migrations.isEmpty()) {
            repository.onInitialized();
            return;
        }

        try (Connection conn = getConnection()) {
            int currentVersion = queryCurrentVersion(conn, moduleId);
            int pending = migrations.size() - currentVersion;

            if (pending <= 0) {
                logger.fine("[DataManager] %s - schema up to date (v%d)".formatted(moduleId, currentVersion));
                repository.onInitialized();
                return;
            }

            conn.setAutoCommit(false);
            try {
                for (int i = currentVersion; i < migrations.size(); i++) {
                    int newVersion = i + 1;
                    logger.info("[DataManager] %s - applying migration v%d".formatted(moduleId, newVersion));

                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(migrations.get(i));
                    }
                    try (PreparedStatement ps = conn.prepareStatement(INSERT_MIGRATION)) {
                        ps.setString(1, moduleId);
                        ps.setInt(2, newVersion);
                        ps.executeUpdate();
                    }
                }
                conn.commit();
                logger.info("[DataManager] %s - migrated to v%d".formatted(moduleId, migrations.size()));
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.severe("[DataManager] Migration failed for module '%s': %s".formatted(moduleId, e.getMessage()));
            throw new RuntimeException("Schema migration failed for module: " + moduleId, e);
        }

        repository.onInitialized();
    }

    public void close() {
        if (!dataSource.isClosed()) {
            dataSource.close();
            logger.info("DataManager pool closed.");
        }
    }

    private void ensureMigrationsTable() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_MIGRATIONS_TABLE);
        } catch (SQLException e) {
            logger.severe("Failed to create schema_migrations table: " + e.getMessage());
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

    private static Method resolveMethod(@NonNull ConfigManager config) {
        // TODO: Change this when I add more methods
        return Method.SQLITE;
    }

    private @NonNull HikariDataSource buildPool(@NonNull File dataFolder, @NonNull ConfigManager config) {
        HikariConfig hc = new HikariConfig();

        File dbFolder = new File(dataFolder, "data");
        dbFolder.mkdirs();
        File dbFile = new File(dbFolder, "tchat.db");

        hc.setJdbcUrl("jdbc:sqlite:%s".formatted(dbFile.getAbsolutePath()));
        hc.setDriverClassName("org.sqlite.JDBC");

        hc.setMaximumPoolSize(1);
        hc.setMinimumIdle(1);
        hc.setConnectionTimeout(5_000);

        hc.addDataSourceProperty("journal_mode", "WAL");
        hc.addDataSourceProperty("synchronous", "NORMAL");
        hc.addDataSourceProperty("foreign_keys", "ON");

        hc.setPoolName("TChat-HikariPool");
        hc.addDataSourceProperty("cachePrepStmts", "true");
        hc.addDataSourceProperty("prepStmtCacheSize", "64");

        return new HikariDataSource(hc);
    }
}