package com.filizes.backpacklimit.databasemanager;

import com.filizes.backpacklimit.Main;
import com.filizes.backpacklimit.config.Messages;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.logging.Logger;

public class DatabaseManager {

    private final Main plugin;
    private HikariDataSource dataSource;


    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
    }

    public void connect() {

        FileConfiguration config = plugin.getConfig();
        String host = config.getString("database.host");
        int port = config.getInt("database.port");
        String database = config.getString("database.database");
        String username = config.getString("database.username");
        String password = config.getString("database.password");
        String tableName = config.getString("database.table");

        dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(10);
        dataSource.setConnectionTimeout(30000);

        try (Connection connection = dataSource.getConnection()) {
            if (connection != null) {
                Logger.getLogger("BackPackLimit").info(Messages.DB_CONNECT_SUCCESS);
                createTables(tableName);
            } else {
                Logger.getLogger("BackPackLimit").severe(Messages.DB_CONNECT_FAILURE);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe(Messages.DB_CONNECT_FAILURE);
        }
    }

    public void createTables(String tableName) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + tableName + " ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "player_name VARCHAR(16) NOT NULL UNIQUE,"
                    + "backpack_limit INT NOT NULL DEFAULT " + plugin.getConfig().getInt("default-backpack-limit") + ");");
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe(Messages.DB_CREATE_TABLE_FAILURE.replace("{table}", tableName));
        }
    }

    public void disconnect() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    public int getPlayerBackpackLimit(String playerName) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT backpack_limit FROM " + plugin.getConfig().getString("database.table") + " WHERE player_name = ?")) {
            statement.setString(1, playerName);

            ResultSet results = statement.executeQuery();

            if (results.next()) {
                return results.getInt("backpack_limit");
            } else {
                return addPlayerToDatabase(playerName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe(Messages.DB_GET_LIMIT_FAILURE.replace("{player}", playerName));
            return plugin.getConfig().getInt("default-backpack-limit");
        }
    }

    public int addPlayerToDatabase(String playerName) {
        int defaultBackpackLimit = plugin.getConfig().getInt("default-backpack-limit");
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO " + plugin.getConfig().getString("database.table") + " (player_name, backpack_limit) VALUES (?, ?)")) {
            statement.setString(1, playerName);
            statement.setInt(2, defaultBackpackLimit);
            statement.executeUpdate();

            Logger.getLogger("BackPackLimit").info(Messages.DB_ADD_PLAYER_SUCCESS.replace("{player}", playerName).replace("{limit}", String.valueOf(defaultBackpackLimit)));
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe(Messages.DB_ADD_PLAYER_FAILURE.replace("{player}", playerName));
        }
        return defaultBackpackLimit;
    }

    public void updatePlayerBackpackLimit(String playerName, int newBackpackLimit) {
        if (newBackpackLimit < 1) {
            Logger.getLogger("BackPackLimit").warning(Messages.DB_LIMIT_BELOW_ONE_WARNING);
            return;
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE " + plugin.getConfig().getString("database.table") + " SET backpack_limit = ? WHERE player_name = ?")) {
            statement.setInt(1, newBackpackLimit);
            statement.setString(2, playerName);
            int rowsUpdated = statement.executeUpdate();

            if (rowsUpdated == 0) {
                Logger.getLogger("BackPackLimit").warning(Messages.DB_PLAYER_NOT_FOUND_WARNING.replace("{player}", playerName));
                addPlayerToDatabase(playerName);
            } else {
                Logger.getLogger("BackPackLimit").info("Лимит рюкзака игрока " + playerName + " обновлен на " + newBackpackLimit);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe(Messages.DB_UPDATE_LIMIT_FAILURE.replace("{player}", playerName));
        }
    }

    public void increasePlayerBackpackLimit(String playerName, int amount) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE " + plugin.getConfig().getString("database.table") + " SET backpack_limit = backpack_limit + ? WHERE player_name = ?")) {
            statement.setInt(1, amount);
            statement.setString(2, playerName);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe(Messages.DB_INCREASE_LIMIT_FAILURE.replace("{player}", playerName));
        }
    }

    public void decreasePlayerBackpackLimit(String playerName, int amount) {
        if (amount <= 0) {
            Logger.getLogger(Messages.DB_LIMIT_BELOW_ONE_WARNING);
            return;
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE " + plugin.getConfig().getString("database.table") + " SET backpack_limit = backpack_limit - ? WHERE player_name = ?")) {
            statement.setInt(1, amount);
            statement.setString(2, playerName);
            int rowsUpdated = statement.executeUpdate();

            if (rowsUpdated == 0) {
                Logger.getLogger(Messages.DB_PLAYER_NOT_FOUND_WARNING.replace("{player}", playerName));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe(Messages.DB_DECREASE_LIMIT_FAILURE.replace("{player}", playerName));
        }
    }
}
