package com.filizes.backpacklimit.commands;

import com.filizes.backpacklimit.config.Messages;
import com.filizes.backpacklimit.databasemanager.DatabaseManager;
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
                sender.sendMessage(ChatColor.RED + "Используй: /playerbackpacklimitremove <player> <amount>");
                return true;
            }

            String targetPlayerName = args[0];
            int removeBackpackLimit;

            try {
                removeBackpackLimit = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Количество должно быть в цифрах.");
                return true;
            }

            if (removeBackpackLimit <= 0) {
                sender.sendMessage(ChatColor.RED + "Сумма должна быть больше нуля.");
                return true;
            }

            databaseManager.decreasePlayerBackpackLimit(targetPlayerName, removeBackpackLimit);

            if (sender instanceof Player) {
                String message = Messages.DB_DECREASE_LIMIT.replace("{player}", targetPlayerName);
                sender.sendMessage(message);
            }
            return true;
        }
        return false;
    }
}
