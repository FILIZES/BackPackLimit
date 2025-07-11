package com.filizes.backpacklimit.database.impl;

import com.filizes.backpacklimit.Main;
import com.filizes.backpacklimit.config.settings.LimitSettings;
import com.filizes.backpacklimit.database.AbstractDatabaseManager;
import com.filizes.backpacklimit.database.exception.DatabaseInitializationException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.function.Function;

public final class SQLiteManager extends AbstractDatabaseManager {

    public SQLiteManager(Main plugin, ConfigurationSection dbConfig, LimitSettings limitSettings, Executor dbExecutor) {
        super(dbConfig, createDataSource(plugin, dbConfig), limitSettings, dbExecutor);
    }

    private static HikariDataSource createDataSource(Main plugin, ConfigurationSection config) {
        Path dbPath = plugin.getDataFolder().toPath().resolve(config.getString("file", "backpacklimit.db"));

        try {
            Path parentDir = dbPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
        } catch (IOException e) {
            throw new DatabaseInitializationException("Не удалось создать директорию для базы данных SQLite.", e);
        }

        var hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("BackpackLimit-SQLite-Pool");
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbPath);
        hikariConfig.setMaximumPoolSize(1);
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.addDataSourceProperty("journal_mode", "WAL");
        hikariConfig.addDataSourceProperty("synchronous", "NORMAL");

        return new HikariDataSource(hikariConfig);
    }

    @Override
    protected Function<String, String> getQuoter() {
        return s -> "\"" + s + "\"";
    }

    @Override
    protected String getUpsertQueryTemplate() {
        return """
               INSERT INTO %s (player_uuid, player_name, backpack_limit) VALUES (?, ?, ?)
               ON CONFLICT(player_uuid) DO UPDATE SET
                   backpack_limit = excluded.backpack_limit,
                   player_name = excluded.player_name;
               """;
    }

    @Override
    protected String getEnsureExistsQueryTemplate() {
        return "INSERT OR IGNORE INTO %s (player_uuid, player_name, backpack_limit) VALUES (?, ?, ?);";
    }

    @Override
    protected String getRemoveLimitQueryTemplate() {
        return "UPDATE %s SET backpack_limit = MAX(0, backpack_limit - ?) WHERE player_uuid = ?;";
    }
}