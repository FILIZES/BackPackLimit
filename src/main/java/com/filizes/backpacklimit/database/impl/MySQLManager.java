package com.filizes.backpacklimit.database.impl;

import com.filizes.backpacklimit.config.settings.LimitSettings;
import com.filizes.backpacklimit.database.AbstractDatabaseManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;

import java.util.concurrent.Executor;
import java.util.function.Function;

public final class MySQLManager extends AbstractDatabaseManager {

    public MySQLManager(ConfigurationSection dbConfig, LimitSettings limitSettings, Executor dbExecutor) {
        super(dbConfig, createDataSource(dbConfig), limitSettings, dbExecutor);
    }

    private static HikariDataSource createDataSource(ConfigurationSection config) {
        var hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("BackpackLimit-MySQL-Pool");
        hikariConfig.setJdbcUrl("jdbc:mysql://%s:%d/%s".formatted(
                config.getString("host", "localhost"),
                config.getInt("port", 3306),
                config.getString("database")));
        hikariConfig.setUsername(config.getString("username"));
        hikariConfig.setPassword(config.getString("password"));

        hikariConfig.setConnectionTimeout(config.getLong("timeouts.connection", 30000));
        hikariConfig.setMaxLifetime(config.getLong("timeouts.max-lifetime", 1800000));
        hikariConfig.setMaximumPoolSize(config.getInt("pool-size", 10));

        ConfigurationSection properties = config.getConfigurationSection("properties");
        if (properties != null) {
            properties.getValues(false).forEach(hikariConfig::addDataSourceProperty);
        }

        return new HikariDataSource(hikariConfig);
    }

    @Override
    protected Function<String, String> getQuoter() {
        return s -> "`" + s + "`";
    }

    @Override
    protected String getUpsertQueryTemplate() {
        return """
               INSERT INTO %s (player_uuid, player_name, backpack_limit) VALUES (?, ?, ?)
               ON DUPLICATE KEY UPDATE
                   backpack_limit = VALUES(backpack_limit),
                   player_name = VALUES(player_name);
               """;
    }

    @Override
    protected String getEnsureExistsQueryTemplate() {
        return "INSERT IGNORE INTO %s (player_uuid, player_name, backpack_limit) VALUES (?, ?, ?);";
    }

    @Override
    protected String getRemoveLimitQueryTemplate() {
        return "UPDATE %s SET backpack_limit = GREATEST(0, backpack_limit - ?) WHERE player_uuid = ?;";
    }
}