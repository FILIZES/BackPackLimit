package com.filizes.backpacklimit.databasemanager.interfaces;

public interface DatabaseManager {
    void connect();
    void disconnect();
    void createTables(String tableName);
    int getPlayerBackpackLimit(String playerName);
    void updatePlayerBackpackLimit(String playerName, int newLimit);
    void addPlayerToDatabase(String playerName);
    void increasePlayerBackpackLimit(String playerName, int amount);
    void decreasePlayerBackpackLimit(String playerName, int amount);
}