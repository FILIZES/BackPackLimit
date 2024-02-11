package com.backpacklimit;

import com.backpacklimit.Commands.CommandManager;
import com.backpacklimit.DatabaseManager.DatabaseManager;
import com.backpacklimit.Placeholder.PlaceholderAPIExpansion;
import com.backpacklimit.Player.PlayerManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin implements Listener {

    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {

        saveDefaultConfig();
        reloadConfig();


        databaseManager = new DatabaseManager(this);
        databaseManager.connect();

        PlayerManager playerManager = new PlayerManager(this, databaseManager);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(playerManager, this);
        getCommand("playerbackpacklimit").setExecutor(new CommandManager(databaseManager));
        getCommand("playerbackpacklimit1").setExecutor(new CommandManager(databaseManager));
        getCommand("playerbackpacklimitremove").setExecutor(new CommandManager(databaseManager));
        getCommand("backpacklimitinfo").setExecutor(new CommandManager(databaseManager));
    }

    @Override
    public void onDisable() {
        databaseManager.disconnect();
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPIExpansion(this).register();
        }
    }

    public DatabaseManager DatabaseManager() {
        return databaseManager;
    }
}