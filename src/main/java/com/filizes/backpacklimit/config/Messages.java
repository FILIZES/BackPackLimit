package com.filizes.backpacklimit.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

public class Messages {
    public static String INVENTORY_LIMIT;
    public static String INVENTORY_OVER_LIMIT_DROP;
    public static String INVENTORY_OVER_LIMIT_MOVE;
    public static String INVENTORY_OVER_LIMIT_PICKUP;

    public static String DB_CONNECT_SUCCESS;
    public static String DB_CONNECT_FAILURE;
    public static String DB_CREATE_TABLE_FAILURE;
    public static String DB_ADD_PLAYER_SUCCESS;
    public static String DB_ADD_PLAYER_FAILURE;
    public static String DB_GET_LIMIT_FAILURE;
    public static String DB_UPDATE_LIMIT_FAILURE;
    public static String DB_INCREASE_LIMIT_FAILURE;
    public static String DB_DECREASE_LIMIT_FAILURE;
    public static String DB_LIMIT_BELOW_ONE_WARNING;
    public static String DB_PLAYER_NOT_FOUND_WARNING;
    public static String DB_SET_LIMIT;
    public static String DB_DECREASE_LIMIT;
    public static String DB_CURRENT_LIMIT;
    public static String DB_INCREASE_LIMIT;

    public static void loadMessages(FileConfiguration config) {
        INVENTORY_LIMIT = translateColor(config.getString("inventory_limit", "&aВаш лимит инвентаря: &6{limit}"));
        INVENTORY_OVER_LIMIT_DROP = translateColor(config.getString("inventory_over_limit_drop", "&cПревышен лимит инвентаря. Удалено {amount} {item}."));
        INVENTORY_OVER_LIMIT_MOVE = translateColor(config.getString("inventory_over_limit_move", "&cПревышен лимит инвентаря. Невозможно переместить {item}."));
        INVENTORY_OVER_LIMIT_PICKUP = translateColor(config.getString("inventory_over_limit_pickup", "&cПревышен лимит инвентаря. Невозможно подобрать {item}."));

        DB_CONNECT_SUCCESS = translateColor(config.getString("database.connect_success", "Успешно подключено к базе данных MySQL!"));
        DB_CONNECT_FAILURE = translateColor(config.getString("database.connect_failure", "Не удалось подключиться к базе данных MySQL!"));
        DB_CREATE_TABLE_FAILURE = translateColor(config.getString("database.create_table_failure", "Не удалось создать таблицу {table}!"));
        DB_ADD_PLAYER_SUCCESS = translateColor(config.getString("database.add_player_success", "Игрок {player} добавлен с лимитом рюкзака {limit}"));
        DB_ADD_PLAYER_FAILURE = translateColor(config.getString("database.add_player_failure", "Не удалось добавить игрока {player} в базу данных!"));
        DB_GET_LIMIT_FAILURE = translateColor(config.getString("database.get_limit_failure", "Не удалось получить лимит рюкзака игрока {player} из базы данных!"));
        DB_UPDATE_LIMIT_FAILURE = translateColor(config.getString("database.update_limit_failure", "Не удалось обновить лимит рюкзака игрока {player} в базе данных!"));
        DB_INCREASE_LIMIT_FAILURE = translateColor(config.getString("database.increase_limit_failure", "Не удалось увеличить лимит рюкзака игрока {player} в базе данных!"));
        DB_DECREASE_LIMIT_FAILURE = translateColor(config.getString("database.decrease_limit_failure", "Не удалось уменьшить лимит рюкзака игрока {player} в базе данных!"));
        DB_LIMIT_BELOW_ONE_WARNING = translateColor(config.getString("database.limit_below_one_warning", "Лимит рюкзака не может быть меньше 1!"));
        DB_PLAYER_NOT_FOUND_WARNING = translateColor(config.getString("database.player_not_found_warning", "Игрок {player} не найден в базе данных!"));

        DB_SET_LIMIT = translateColor(config.getString("database.set_limit", "&aЛимит рюкзака для игрока {player} установлен на {limit} слотов."));
        DB_DECREASE_LIMIT = translateColor(config.getString("database.decrease_limit", "&aЛимит рюкзака для игрока {player} уменьшен на {limit} слотов."));
        DB_CURRENT_LIMIT = translateColor(config.getString("database.current_limit", "&aЛимит рюкзака для игрока {player} составляет {limit} слотов."));
        DB_INCREASE_LIMIT = translateColor(config.getString("database.increase_limit", "&aЛимит рюкзака для игрока {player} увеличен на {amount} слот."));
    }

    private static String translateColor(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
