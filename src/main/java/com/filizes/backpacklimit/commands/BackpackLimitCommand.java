package com.filizes.backpacklimit.commands;

import com.filizes.backpacklimit.config.Messages;
import com.filizes.backpacklimit.databasemanager.interfaces.DatabaseManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BackpackLimitCommand implements CommandExecutor {

    private final DatabaseManager databaseManager;

    public BackpackLimitCommand(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("backpacklimit.use")) {
            sender.sendMessage(ChatColor.RED + "У вас нет разрешения на выполнение этой команды.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Используй: /backpacklimit <info|set|add|remove> [<player> <amount>]");
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "info":
                return handleInfoCommand(sender, args);
            case "set":
                return handleSetCommand(sender, args);
            case "add":
                return handleAddCommand(sender, args);
            case "remove":
                return handleRemoveCommand(sender, args);
            default:
                sender.sendMessage(ChatColor.RED + "Неизвестное действие. Используй: /backpacklimit <info|set|add|remove>");
                return true;
        }
    }

    private boolean handleInfoCommand(CommandSender sender, String[] args) {
        if (args.length != 2 || !sender.hasPermission("backpacklimit.info")) {
            sender.sendMessage(ChatColor.RED + "Используй: /backpacklimit info <player>");
            return true;
        }

        String targetPlayerName = args[1];
        int playerBackpackLimit = databaseManager.getPlayerBackpackLimit(targetPlayerName);
        String message = Messages.DB_CURRENT_LIMIT.replace("{player}", targetPlayerName).replace("{limit}", String.valueOf(playerBackpackLimit));
        sender.sendMessage(message);
        return true;
    }

    private boolean handleSetCommand(CommandSender sender, String[] args) {
        if (args.length != 3 || !sender.hasPermission("backpacklimit.set")) {
            sender.sendMessage(ChatColor.RED + "Используй: /backpacklimit set <player> <amount>");
            return true;
        }

        String targetPlayerName = args[1];
        int newBackpackLimit;

        try {
            newBackpackLimit = Integer.parseInt(args[2]);
            if (newBackpackLimit <= 0) {
                sender.sendMessage(ChatColor.RED + "Сумма должна быть больше нуля.");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Количество должно быть в цифрах.");
            return true;
        }

        databaseManager.updatePlayerBackpackLimit(targetPlayerName, newBackpackLimit);
        String message = Messages.DB_SET_LIMIT.replace("{player}", targetPlayerName).replace("{limit}", String.valueOf(newBackpackLimit));
        sender.sendMessage(message);
        return true;
    }

    private boolean handleAddCommand(CommandSender sender, String[] args) {
        if (args.length != 3 || !sender.hasPermission("backpacklimit.add")) {
            sender.sendMessage(ChatColor.RED + "Используй: /backpacklimit add <player> <amount>");
            return true;
        }

        String targetPlayerName = args[1];
        int amount;

        try {
            amount = Integer.parseInt(args[2]);
            if (amount <= 0) {
                sender.sendMessage(ChatColor.RED + "Сумма должна быть больше нуля.");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Количество должно быть в цифрах.");
            return true;
        }

        databaseManager.increasePlayerBackpackLimit(targetPlayerName, amount);
        String message = Messages.DB_INCREASE_LIMIT.replace("{player}", targetPlayerName).replace("{amount}", String.valueOf(amount));
        sender.sendMessage(message);
        return true;
    }

    private boolean handleRemoveCommand(CommandSender sender, String[] args) {
        if (args.length != 3 || !sender.hasPermission("backpacklimit.remove")) {
            sender.sendMessage(ChatColor.RED + "Используй: /backpacklimit remove <player> <amount>");
            return true;
        }

        String targetPlayerName = args[1];
        int removeBackpackLimit;

        try {
            removeBackpackLimit = Integer.parseInt(args[2]);
            if (removeBackpackLimit <= 0) {
                sender.sendMessage(ChatColor.RED + "Сумма должна быть больше нуля.");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Количество должно быть в цифрах.");
            return true;
        }

        databaseManager.decreasePlayerBackpackLimit(targetPlayerName, removeBackpackLimit);
        String message = Messages.DB_DECREASE_LIMIT.replace("{player}", targetPlayerName).replace("{amount}", String.valueOf(removeBackpackLimit));
        sender.sendMessage(message);
        return true;
    }

}
