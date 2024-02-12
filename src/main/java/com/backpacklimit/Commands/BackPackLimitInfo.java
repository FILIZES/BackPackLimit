package com.backpacklimit.Commands;

import com.backpacklimit.DatabaseManager.DatabaseManager;
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
        if (cmd.getName().equalsIgnoreCase("backpacklimitinfo")) {
            if (!sender.hasPermission("backpacklimit.info") || args.length != 1) {
                return true;
            }

            String targetPlayerName = args[0];
            int playerBackpackLimit = databaseManager.getPlayerBackpackLimit(targetPlayerName);

            if (sender instanceof Player) {
                String message = ChatColor.GREEN + "Лимит рюкзака для игрока " + targetPlayerName + " составляет " + playerBackpackLimit + " слотов.";
                sender.sendMessage(message);
            }
            return true;
        }

        return false;
    }
}
