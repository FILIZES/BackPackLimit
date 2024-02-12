package com.backpacklimit.Commands;

import com.backpacklimit.DatabaseManager.DatabaseManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RemoveBackPack implements CommandExecutor {

    private final DatabaseManager databaseManager;

    public RemoveBackPack(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("playerbackpacklimitremove")) {
            if (!sender.hasPermission("backpacklimit.remove") || args.length != 2) {
                return true;
            }

            String targetPlayerName = args[0];
            int removeBackpackLimit;

            try {
                removeBackpackLimit = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                return true;
            }

            if (removeBackpackLimit <= 0) {
                return true;
            }

            databaseManager.decreasePlayerBackpackLimit(targetPlayerName, removeBackpackLimit);

            if (sender instanceof Player) {
                String message = ChatColor.GREEN + "Лимит рюкзака для игрока " + targetPlayerName + " уменьшен на " + removeBackpackLimit + " слотов.";
                sender.sendMessage(message);
            }
            return true;
        }
        return false;
    }
}
