package com.backpacklimit.Commands;

import com.backpacklimit.DatabaseManager.DatabaseManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetBackPackLimit implements CommandExecutor {

    private final DatabaseManager databaseManager;

    public SetBackPackLimit(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("playerbackpacklimit")) {
            if (args.length != 2 || !sender.hasPermission("backpacklimit.set")) {
                return true;
            }

            String targetPlayerName = args[0];
            int newBackpackLimit;

            try {
                newBackpackLimit = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                return true;
            }

            if (newBackpackLimit <= 0) {
                return true;
            }

            databaseManager.updatePlayerBackpackLimit(targetPlayerName, newBackpackLimit);

            if (sender instanceof Player) {
                String message = ChatColor.GREEN + "Лимит рюкзака для игрока " + targetPlayerName + " установлен на " + newBackpackLimit + " слотов.";
                sender.sendMessage(message);
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("playerbackpacklimit1")) {

            if (!sender.hasPermission("backpacklimit.add") || args.length != 1) {
                return true;
            }

            String targetPlayerName = args[0];
            databaseManager.increasePlayerBackpackLimit(targetPlayerName);

            if (sender instanceof Player) {
                String message = ChatColor.GREEN + "Лимит рюкзака для игрока " + targetPlayerName + " увеличен на 1 слот.";
                sender.sendMessage(message);
            }
            return true;
        }

        return false;
    }
}
