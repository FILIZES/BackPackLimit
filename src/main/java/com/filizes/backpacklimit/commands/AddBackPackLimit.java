package com.filizes.backpacklimit.commands;

import com.filizes.backpacklimit.config.Messages;
import com.filizes.backpacklimit.databasemanager.DatabaseManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AddBackPackLimit implements CommandExecutor {
    private final DatabaseManager databaseManager;

    public AddBackPackLimit(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("playerbackpacklimitadd")) {
            if (args.length != 2) {
                sender.sendMessage(ChatColor.RED + "Используйте: /playerbackpacklimitadd <player> <amount>");
                return true;
            }

            if (sender instanceof Player && !sender.hasPermission("backpacklimit.add")) {
                sender.sendMessage(ChatColor.RED + "У вас нет прав для выполнения этой команды.");
                return true;
            }

            String targetPlayerName = args[0];
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Количество должно быть числом.");
                return true;
            }

            if (amount <= 0) {
                sender.sendMessage(ChatColor.RED + "Сумма должна быть больше нуля.");
                return true;
            }

            databaseManager.increasePlayerBackpackLimit(targetPlayerName, amount);

            String message = Messages.DB_INCREASE_LIMIT.replace("{player}", targetPlayerName).replace("{amount}", String.valueOf(amount));
            sender.sendMessage(message);
            return true;
        }

        return false;
    }
}