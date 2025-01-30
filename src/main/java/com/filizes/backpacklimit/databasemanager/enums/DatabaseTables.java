package com.filizes.backpacklimit.databasemanager.enums;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

@Getter
public enum DatabaseTables {

    PLAYER_DATA("players");

    private String tableName;

    DatabaseTables(String tableName) {
        this.tableName = tableName;
    }

    public static void initialize(FileConfiguration config) {
        PLAYER_DATA.tableName = config.getString("database.table", "players");
    }
}