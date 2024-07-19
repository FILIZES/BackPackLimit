package com.filizes.backpacklimit.commands;

import com.filizes.backpacklimit.config.Messages;
import com.filizes.backpacklimit.databasemanager.DatabaseManager;
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
        if (cmd.getName().equalsIgnoreCase("playerbackpacklimitset")) {
            if (args.length != 2 || !sender.hasPermission("backpacklimit.set")) {
                sender.sendMessage(ChatColor.RED + "Используй: /playerbackpacklimitset <player> <amount>");
                return true;
            }

            String targetPlayerName = args[0];
            int newBackpackLimit;

            try {
                newBackpackLimit = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Количество должно быть в цифрах.");
                return true;
            }

            if (newBackpackLimit <= 0) {
                sender.sendMessage(ChatColor.RED + "Сумма должна быть больше нуля.");
                return true;
            }

            databaseManager.updatePlayerBackpackLimit(targetPlayerName, newBackpackLimit);

            if (sender instanceof Player) {
                String message = Messages.DB_SET_LIMIT.replace("{player}", targetPlayerName).replace("{limit}", String.valueOf(newBackpackLimit));
                sender.sendMessage(message);
            }
            return true;
        }
        return false;
    }
}
