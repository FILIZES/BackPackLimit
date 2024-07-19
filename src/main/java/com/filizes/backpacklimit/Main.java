package com.filizes.backpacklimit;

import com.filizes.backpacklimit.config.Messages;
import com.filizes.backpacklimit.databasemanager.DatabaseManager;
import com.filizes.backpacklimit.placeholder.PlaceholderAPIExpansion;
import com.filizes.backpacklimit.player.PlayerManager;
import com.filizes.backpacklimit.config.MessagesConfig;
import com.filizes.backpacklimit.commands.AddBackPackLimit;
import com.filizes.backpacklimit.commands.BackPackLimitInfo;
import com.filizes.backpacklimit.commands.RemoveBackPack;
import com.filizes.backpacklimit.commands.SetBackPackLimit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin implements Listener {

    private DatabaseManager databaseManager;
    private MessagesConfig messagesConfig;

    @Override
    public void onEnable() {

        AutoUpdater autoUpdater = new AutoUpdater(this);
        autoUpdater.checkForUpdates();

        messagesConfig = new MessagesConfig(this);
        Messages.loadMessages(messagesConfig.getMessagesConfig());

        saveDefaultConfig();
        reloadConfig();

        databaseManager = new DatabaseManager(this);
        databaseManager.connect();

        PlayerManager playerManager = new PlayerManager(this, databaseManager);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(playerManager, this);
        getServer().getPluginManager().registerEvents(messagesConfig,this);
        getServer().getPluginManager().registerEvents(autoUpdater,this);
        getCommand("playerbackpacklimitset").setExecutor(new SetBackPackLimit(databaseManager));
        getCommand("playerbackpacklimitadd").setExecutor(new AddBackPackLimit(databaseManager));
        getCommand("playerbackpacklimitremove").setExecutor(new RemoveBackPack(databaseManager));
        getCommand("playerbackpacklimitinfo").setExecutor(new BackPackLimitInfo(databaseManager));

    }

    @Override
    public void onDisable() {
        databaseManager.disconnect();
        messagesConfig.saveMessagesConfig();
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
