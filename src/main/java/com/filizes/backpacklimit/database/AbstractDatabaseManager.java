package com.filizes.backpacklimit.database;

import com.filizes.backpacklimit.config.settings.LimitSettings;
import com.filizes.backpacklimit.database.exception.DatabaseInitializationException;
import com.filizes.backpacklimit.database.exception.DatabaseQueryException;
import com.filizes.backpacklimit.database.identificator.SQLIdentifier;
import com.filizes.backpacklimit.database.identificator.safe.SafeQuery;
import com.filizes.backpacklimit.database.interfaces.DatabaseManager;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class AbstractDatabaseManager implements DatabaseManager {

    private static final String CREATE_TABLE_TEMPLATE = """
            CREATE TABLE IF NOT EXISTS %s (
                player_uuid VARCHAR(36) PRIMARY KEY NOT NULL,
                player_name VARCHAR(16) NOT NULL,
                backpack_limit INT NOT NULL
            );""";
    private static final String GET_PLAYER_LIMIT_QUERY = "SELECT backpack_limit FROM %s WHERE player_uuid = ?;";
    private static final String GET_PLAYER_UUID_QUERY = "SELECT player_uuid FROM %s WHERE player_name = ? LIMIT 1;";
    private static final String ADD_PLAYER_LIMIT_QUERY = "UPDATE %s SET backpack_limit = backpack_limit + ? WHERE player_uuid = ?;";
    private static final String FIND_PLAYERS_QUERY = "SELECT player_name FROM %s WHERE player_name LIKE ? LIMIT 10;";

    protected final HikariDataSource dataSource;
    @Getter(AccessLevel.PROTECTED)
    protected final int defaultLimit;
    private final Executor asyncExecutor;

    private final SafeQuery queryCreateTable;
    private final SafeQuery queryGetPlayerLimit;
    private final SafeQuery queryGetPlayerUUID;
    private final SafeQuery queryAddPlayerLimit;
    private final SafeQuery queryRemovePlayerLimit;
    private final SafeQuery querySetPlayerLimit;
    private final SafeQuery queryEnsurePlayerExists;
    private final SafeQuery queryFindPlayersByNamePrefix;

    @FunctionalInterface
    protected interface StatementPreparer {
        void prepare(PreparedStatement stmt) throws SQLException;
    }

    @FunctionalInterface
    protected interface ResultSetMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    protected AbstractDatabaseManager(ConfigurationSection dbConfig, HikariDataSource dataSource, LimitSettings limitSettings, Executor asyncExecutor) {
        this.dataSource = dataSource;
        this.defaultLimit = limitSettings.defaultLimit();
        this.asyncExecutor = asyncExecutor;

        SQLIdentifier tableName = initializeTableName(dbConfig);

        this.queryCreateTable = SafeQuery.from(CREATE_TABLE_TEMPLATE, tableName);
        this.queryGetPlayerLimit = SafeQuery.from(GET_PLAYER_LIMIT_QUERY, tableName);
        this.queryGetPlayerUUID = SafeQuery.from(GET_PLAYER_UUID_QUERY, tableName);
        this.queryAddPlayerLimit = SafeQuery.from(ADD_PLAYER_LIMIT_QUERY, tableName);
        this.queryFindPlayersByNamePrefix = SafeQuery.from(FIND_PLAYERS_QUERY, tableName);
        this.queryRemovePlayerLimit = SafeQuery.from(getRemoveLimitQueryTemplate(), tableName);
        this.querySetPlayerLimit = SafeQuery.from(getUpsertQueryTemplate(), tableName);
        this.queryEnsurePlayerExists = SafeQuery.from(getEnsureExistsQueryTemplate(), tableName);
    }

    protected abstract Function<String, String> getQuoter();
    protected abstract String getUpsertQueryTemplate();
    protected abstract String getEnsureExistsQueryTemplate();
    protected abstract String getRemoveLimitQueryTemplate();

    private SQLIdentifier initializeTableName(ConfigurationSection config) {
        String configuredName = config.getString("table", "backpack_limits");
        try {
            return SQLIdentifier.of(configuredName, getQuoter());
        } catch (IllegalArgumentException e) {
            log.error("!!! ОПАСНОСТЬ: Неверное имя таблицы в файле config.yml: '{}'", configuredName);
            log.error("!!! Это может быть попытка SQL-инъекции! Используется имя таблицы по умолчанию 'backpack_limits'.");
            return SQLIdentifier.of("backpack_limits", getQuoter());
        }
    }

    @Override
    public final void connect() {
        if (dataSource.isClosed()) {
            throw new DatabaseInitializationException("Пул соединений уже закрыт.");
        }
        executeUpdate(queryCreateTable, stmt -> {})
                .thenRun(() -> log.info("База данных '{}' успешно инициализирована.", dataSource.getPoolName()))
                .exceptionally(ex -> {
                    log.error("Ошибка при создании таблицы в базе данных.", ex.getCause() != null ? ex.getCause() : ex);
                    return null;
                });
    }

    @Override
    public final void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private <T> CompletableFuture<T> executeQuery(SafeQuery safeQuery, StatementPreparer preparer, ResultSetMapper<T> mapper) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(safeQuery.sql())) {
                preparer.prepare(stmt);
                try (ResultSet rs = stmt.executeQuery()) {
                    return mapper.map(rs);
                }
            } catch (SQLException e) {
                throw new DatabaseQueryException("Не удалось выполнить запрос: " + safeQuery, e);
            }
        }, asyncExecutor);
    }

    private CompletableFuture<Void> executeUpdate(SafeQuery safeQuery, StatementPreparer preparer) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(safeQuery.sql())) {
                preparer.prepare(stmt);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseQueryException("Не удалось выполнить обновление: " + safeQuery, e);
            }
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<Void> setPlayerBackpackLimit(OfflinePlayer player, int newLimit) {
        return executeUpdate(querySetPlayerLimit, stmt -> {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, player.getName());
            stmt.setInt(3, newLimit);
        });
    }

    @Override
    public CompletableFuture<Void> ensurePlayerExists(Player player) {
        return executeUpdate(queryEnsurePlayerExists, stmt -> {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, player.getName());
            stmt.setInt(3, this.defaultLimit);
        });
    }

    @Override
    public CompletableFuture<Void> addPlayerBackpackLimit(OfflinePlayer player, int amount) {
        return executeUpdate(queryAddPlayerLimit, stmt -> {
            stmt.setInt(1, amount);
            stmt.setString(2, player.getUniqueId().toString());
        });
    }

    @Override
    public CompletableFuture<Void> removePlayerBackpackLimit(OfflinePlayer player, int amount) {
        return executeUpdate(queryRemovePlayerLimit, stmt -> {
            stmt.setInt(1, amount);
            stmt.setString(2, player.getUniqueId().toString());
        });
    }

    @Override
    public CompletableFuture<Integer> getPlayerBackpackLimit(UUID playerUUID) {
        return executeQuery(queryGetPlayerLimit,
                stmt -> stmt.setString(1, playerUUID.toString()),
                rs -> rs.next() ? rs.getInt("backpack_limit") : this.defaultLimit
        );
    }

    @Override
    public CompletableFuture<Optional<UUID>> getPlayerUUID(String playerName) {
        return executeQuery(queryGetPlayerUUID,
                stmt -> stmt.setString(1, playerName),
                rs -> rs.next() ? Optional.of(UUID.fromString(rs.getString("player_uuid"))) : Optional.empty()
        );
    }

    @Override
    public CompletableFuture<List<String>> findPlayersByNamePrefix(String prefix) {
        return executeQuery(queryFindPlayersByNamePrefix,
                stmt -> stmt.setString(1, prefix + "%"),
                rs -> {
                    var names = new ArrayList<String>();
                    while (rs.next()) {
                        names.add(rs.getString("player_name"));
                    }
                    return names;
                });
    }
}