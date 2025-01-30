package com.filizes.backpacklimit.databasemanager;

import com.filizes.backpacklimit.Main;
import com.filizes.backpacklimit.databasemanager.enums.DatabaseTables;
import com.filizes.backpacklimit.databasemanager.interfaces.DatabaseManager;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.logging.Logger;

public class DatabaseManagerImpl implements DatabaseManager {
    private final Main plugin;
    private HikariDataSource dataSource;

    public DatabaseManagerImpl(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void connect() {
        String host = plugin.getConfig().getString("database.host");
        int port = plugin.getConfig().getInt("database.port");
        String database = plugin.getConfig().getString("database.database");
        String username = plugin.getConfig().getString("database.username");
        String password = plugin.getConfig().getString("database.password");

        dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(10);

        try (Connection connection = dataSource.getConnection()) {
            if (connection != null) {
                Logger.getLogger("BackPackLimit").info("Successfully connected to database!");
                createTables(DatabaseTables.PLAYER_DATA.getTableName());
            } else {
                Logger.getLogger("BackPackLimit").severe("Failed to connect to database!");
            }
        } catch (SQLException e) {
            Logger.getLogger("BackPackLimit").severe("Connection error: " + e.getMessage());
        }
    }

    @Override
    public void createTables(String tableName) {
        int defaultLimit = plugin.getConfig().getInt("default-backpack-limit");
        String query = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "player_name VARCHAR(16) NOT NULL UNIQUE, " +
                "backpack_limit INT NOT NULL DEFAULT " + defaultLimit + ");";

        executeUpdate(query);
    }

    private void executeUpdate(String query, Object... params) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            Logger.getLogger("BackPackLimit").severe("SQL error: " + e.getMessage());
        }
    }

    @Override
    public int getPlayerBackpackLimit(String playerName) {
        String tableName = DatabaseTables.PLAYER_DATA.getTableName();
        String selectQuery = "SELECT backpack_limit FROM " + tableName + " WHERE player_name = ?";
        int backpackLimit = plugin.getConfig().getInt("default-backpack-limit");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement selectStatement = connection.prepareStatement(selectQuery)) {
            selectStatement.setString(1, playerName);
            ResultSet results = selectStatement.executeQuery();

            if (results.next()) {
                return results.getInt("backpack_limit");
            } else {
                addPlayerToDatabase(playerName);
                return backpackLimit;
            }
        } catch (SQLException e) {
            Logger.getLogger("BackPackLimit").severe("SQL error: " + e.getMessage());
            return backpackLimit;
        }
    }


    @Override
    public void addPlayerToDatabase(String playerName) {
        int defaultLimit = plugin.getConfig().getInt("default-backpack-limit");
        String tableName = DatabaseTables.PLAYER_DATA.getTableName();
        String query = "INSERT INTO " + tableName + " (player_name, backpack_limit) VALUES (?, ?)";

        executeUpdate(query, playerName, defaultLimit);
    }

    @Override
    public void updatePlayerBackpackLimit(String playerName, int newLimit) {
        newLimit = Math.max(0, newLimit);
        String tableName = DatabaseTables.PLAYER_DATA.getTableName();
        String query = "UPDATE " + tableName + " SET backpack_limit = ? WHERE player_name = ?";

        executeUpdate(query, newLimit, playerName);
    }

    @Override
    public void increasePlayerBackpackLimit(String playerName, int amount) {
        int currentLimit = getPlayerBackpackLimit(playerName);
        updatePlayerBackpackLimit(playerName, currentLimit + amount);
    }

    @Override
    public void decreasePlayerBackpackLimit(String playerName, int amount) {
        int currentLimit = getPlayerBackpackLimit(playerName);
        updatePlayerBackpackLimit(playerName, currentLimit - amount);
    }

    @Override
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}