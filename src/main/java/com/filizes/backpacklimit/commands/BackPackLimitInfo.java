package com.filizes.backpacklimit.commands;

import com.filizes.backpacklimit.config.Messages;
import com.filizes.backpacklimit.databasemanager.DatabaseManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BackPackLimitInfo implements CommandExecutor {

    private final DatabaseManager databaseManager;

    public BackPackLimitInfo(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("playerbackpacklimitinfo")) {
            if (!sender.hasPermission("backpacklimit.info") || args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Используй: /playerbackpacklimitinfo <player> ");
                return true;
            }

            String targetPlayerName = args[0];
            int playerBackpackLimit = databaseManager.getPlayerBackpackLimit(targetPlayerName);

            if (sender instanceof Player) {
                String message = Messages.DB_CURRENT_LIMIT.replace("{player}", targetPlayerName).replace("{limit}", String.valueOf(playerBackpackLimit));
                sender.sendMessage(message);
            }
            return true;
        }

        return false;
    }
}
