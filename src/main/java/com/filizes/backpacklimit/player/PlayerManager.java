package com.filizes.backpacklimit.player;

import com.filizes.backpacklimit.Main;
import com.filizes.backpacklimit.config.Messages;
import com.filizes.backpacklimit.databasemanager.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class PlayerManager implements Listener {

    private final DatabaseManager databaseManager;
    private final Map<String, Integer> playerBackpackLimits = new HashMap<>();


    public PlayerManager(Main plugin, DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;

        new BukkitRunnable() {
            @Override
            public void run() {
                checkLimit();
            }
        }.runTaskTimer(plugin, 20L * 5, 20L * 5);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        int playerBackpackLimit = databaseManager.getPlayerBackpackLimit(playerName);
        playerBackpackLimits.put(playerName, playerBackpackLimit);

        Inventory playerInventory = player.getInventory();
        if (playerInventory.getSize() > playerBackpackLimit) {
            dropItem(player);
        }

        String message = Messages.INVENTORY_LIMIT.replace("{limit}", String.valueOf(playerBackpackLimit));
        player.sendMessage(message);
    }

    public void dropItem(Player player) {
        Inventory inventory = player.getInventory();
        int playerBackpackLimit = playerBackpackLimits.get(player.getName());
        int occupiedSlots = getOccupiedSlots(inventory);

        if (occupiedSlots > playerBackpackLimit) {
            int itemsToRemove = occupiedSlots - playerBackpackLimit;
            for (int i = inventory.getSize() - 1; i >= 0 && itemsToRemove > 0; i--) {
                ItemStack itemToRemove = inventory.getItem(i);
                if (itemToRemove != null && itemToRemove.getType() != Material.AIR) {
                    inventory.setItem(i, null);
                    player.getWorld().dropItemNaturally(player.getLocation(), itemToRemove);
                    itemsToRemove--;
                    String dropMessage = Messages.INVENTORY_OVER_LIMIT_DROP
                            .replace("{amount}", String.valueOf(itemToRemove.getAmount()))
                            .replace("{item}", itemToRemove.getType().name().toLowerCase());
                    player.sendMessage(dropMessage);
                }
            }
        }
    }

    public void checkLimit() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            dropItem(player);
        }
    }

    private int getOccupiedSlots(Inventory inventory) {
        int occupiedSlots = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                occupiedSlots++;
            }
        }
        return occupiedSlots;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClickEvent(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int playerBackpackLimit = playerBackpackLimits.get(player.getName());
        int occupiedSlots = getOccupiedSlots(player.getInventory());

        if (occupiedSlots >= playerBackpackLimit) {
            event.setCancelled(true);
            ItemStack cursorItem = event.getCursor();
            if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                String moveMessage = Messages.INVENTORY_OVER_LIMIT_MOVE.replace("{item}", cursorItem.getType().name().toLowerCase());
                player.sendMessage(moveMessage);
                player.getInventory().remove(cursorItem);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPickupItemEvent(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        int playerBackpackLimit = playerBackpackLimits.get(player.getName());
        int occupiedSlots = getOccupiedSlots(player.getInventory());

        if (occupiedSlots >= playerBackpackLimit) {
            event.setCancelled(true);
            String pickupMessage = Messages.INVENTORY_OVER_LIMIT_PICKUP.replace("{item}", event.getItem().getItemStack().getType().name().toLowerCase());
            player.sendMessage(pickupMessage);
        }
    }
}