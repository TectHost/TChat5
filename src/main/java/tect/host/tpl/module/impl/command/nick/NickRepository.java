package tect.host.tpl.module.impl.command.nick;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.data.DataManager;
import tect.host.tpl.data.DataMethod;
import tect.host.tpl.data.Repository;
import tect.host.tpl.util.Utils;

import java.sql.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

public final class NickRepository implements Repository {

    private static final String MIGRATION_V1_SQLITE = """
            CREATE TABLE IF NOT EXISTS player_nicks (
                uuid TEXT NOT NULL PRIMARY KEY,
                nick TEXT NOT NULL,
                updated INTEGER NOT NULL
            )
            """;

    // nickname VARCHAR(32): NickCommand.MAX_NICK_LEN limits the nickname to 32 characters
    private static final String MIGRATION_V1_MARIADB = """
            CREATE TABLE IF NOT EXISTS player_nicks (
                uuid VARCHAR(36) NOT NULL PRIMARY KEY,
                nick VARCHAR(32) NOT NULL,
                updated BIGINT NOT NULL
            )
            """;

    private static final String UPSERT_SQLITE = """
            INSERT OR REPLACE INTO player_nicks (uuid, nick, updated)
            VALUES (?, ?, ?)
            """;

    private static final String UPSERT_MARIADB = """
            INSERT INTO player_nicks (uuid, nick, updated)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE nick = VALUES(nick), updated = VALUES(updated)
            """;

    private static final String SELECT = "SELECT nick FROM player_nicks WHERE uuid = ?";
    private static final String DELETE = "DELETE FROM player_nicks WHERE uuid = ?";

    private final DataManager dataManager;
    private final Executor asyncExecutor;
    private final Logger logger;
    private final String upsertSql;

    private final ConcurrentHashMap<UUID, String> cache = new ConcurrentHashMap<>();

    public NickRepository(@NonNull DataManager dataManager, @NonNull Executor asyncExecutor, @NonNull Logger logger) {
        this.dataManager = dataManager;
        this.asyncExecutor = asyncExecutor;
        this.logger = logger;
        this.upsertSql = dataManager.getMethod() == DataMethod.MARIADB ? UPSERT_MARIADB : UPSERT_SQLITE;
    }

    @Override
    public @NonNull String getModuleId() { return "nick"; }

    @Contract(value = " -> new", pure = true)
    @Override
    public @NonNull @Unmodifiable List<String> getMigrations() {
        return List.of(MIGRATION_V1_SQLITE);
    }

    @Contract(pure = true)
    @Override
    public @NonNull @Unmodifiable List<String> getMigrations(@NonNull DataMethod method) {
        return List.of(method == DataMethod.MARIADB ? MIGRATION_V1_MARIADB : MIGRATION_V1_SQLITE);
    }

    public @NonNull Optional<String> getNickCached(@NonNull UUID uuid) {
        return Optional.ofNullable(cache.get(uuid));
    }

    public void preload(@NonNull UUID uuid) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = dataManager.getConnection(); PreparedStatement ps = conn.prepareStatement(SELECT)) {

                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        cache.put(uuid, rs.getString("nick"));
                    } else {
                        // No nickname
                        cache.remove(uuid);
                    }
                }
            } catch (SQLException e) {
                Utils.log(logger, "SEVERE", "Failed to preload nick for %s: %s".formatted(uuid, e.getMessage()));
            }
        }, asyncExecutor);
    }

    public @NonNull CompletableFuture<Void> setNick(@NonNull UUID uuid, @NonNull String nick) {
        String previous = cache.put(uuid, nick);

        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataManager.getConnection(); PreparedStatement ps = conn.prepareStatement(upsertSql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, nick);
                ps.setLong(3, Instant.now().getEpochSecond());
                ps.executeUpdate();
            } catch (SQLException e) {
                // Revert the cache to its previous state
                if (previous != null) {
                    cache.put(uuid, previous);
                } else {
                    cache.remove(uuid);
                }
                Utils.log(logger, "SEVERE", "Failed to save nick for %s: %s".formatted(uuid, e.getMessage()));
                throw new RuntimeException("DB write failed for setNick", e);
            }
        }, asyncExecutor);
    }

    public @NonNull CompletableFuture<Void> removeNick(@NonNull UUID uuid) {
        String previous = cache.remove(uuid);

        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataManager.getConnection(); PreparedStatement ps = conn.prepareStatement(DELETE)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                // restore nick in cache if db failed
                if (previous != null) cache.put(uuid, previous);
                Utils.log(logger, "SEVERE", "Failed to remove nick for %s: %s".formatted(uuid, e.getMessage()));
                throw new RuntimeException("DB write failed for removeNick", e);
            }
        }, asyncExecutor);
    }

    public void evict(@NonNull UUID uuid) {
        cache.remove(uuid);
    }

    public void invalidateAll() {
        cache.clear();
    }
}