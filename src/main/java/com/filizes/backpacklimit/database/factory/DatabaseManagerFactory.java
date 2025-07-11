package com.filizes.backpacklimit.database.factory;

import com.filizes.backpacklimit.Main;
import com.filizes.backpacklimit.config.settings.LimitSettings;
import com.filizes.backpacklimit.database.exception.DatabaseInitializationException;
import com.filizes.backpacklimit.database.factory.type.StorageType;
import com.filizes.backpacklimit.database.impl.MySQLManager;
import com.filizes.backpacklimit.database.impl.SQLiteManager;
import com.filizes.backpacklimit.database.interfaces.DatabaseManager;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Optional;
import java.util.concurrent.Executor;

@Slf4j
@UtilityClass
public class DatabaseManagerFactory {

    public DatabaseManager createDatabaseManager(Main plugin, LimitSettings limitSettings, Executor dbExecutor) {
        ConfigurationSection dbConfig = Optional.ofNullable(plugin.getConfig().getConfigurationSection("database"))
                .orElseThrow(() -> new DatabaseInitializationException("Секция 'database' отсутствует в config.yml"));

        String typeName = dbConfig.getString("type", "SQLITE");
        StorageType storageType = StorageType.fromString(typeName);

        DatabaseManager manager = switch (storageType) {
            case MYSQL -> new MySQLManager(getRequiredSection(dbConfig, "mysql"), limitSettings, dbExecutor);
            case SQLITE -> new SQLiteManager(plugin, getRequiredSection(dbConfig, "sqlite"), limitSettings, dbExecutor);
        };

        manager.connect();
        return manager;
    }

    private ConfigurationSection getRequiredSection(ConfigurationSection parent, String key) {
        return Optional.ofNullable(parent.getConfigurationSection(key))
                .orElseThrow(() -> new DatabaseInitializationException("Секция 'database.%s' отсутствует в config.yml".formatted(key)));
    }
}