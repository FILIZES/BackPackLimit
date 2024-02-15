package com.backpacklimit;

import com.backpacklimit.Commands.*;
import com.backpacklimit.DatabaseManager.DatabaseManager;
import com.backpacklimit.Placeholder.PlaceholderAPIExpansion;
import com.backpacklimit.Player.PlayerManager;
import org.bukkit.Bukkit;
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
        getCommand("playerbackpacklimit").setExecutor(new SetBackPackLimit(databaseManager));
        getCommand("playerbackpacklimit1").setExecutor(new AddBackPackLimit(databaseManager));
        getCommand("playerbackpacklimitremove").setExecutor(new RemoveBackPack(databaseManager));
        getCommand("backpacklimitinfo").setExecutor(new BackPackLimitInfo(databaseManager));

    }

    @Override
    public void onDisable() {
        databaseManager.disconnect();
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (event.getPlugin().getName().equals("PlaceholderAPI")) {
            new PlaceholderAPIExpansion(this).register();
        }
    }

    public DatabaseManager DatabaseManager() {
        return databaseManager;
    }

}
