package com.filizes.backpacklimit.player;

import com.filizes.backpacklimit.Main;
import com.filizes.backpacklimit.config.Messages;
import com.filizes.backpacklimit.databasemanager.interfaces.DatabaseManager;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.Sets;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class PlayerManager implements Listener {

    private final DatabaseManager databaseManager;
    private final Set<String> cooldownMessages = Sets.newConcurrentHashSet();
    private final Map<UUID, Integer> inventorySnapshot = new HashMap<>();
    private final Main plugin;
    private LoadingCache<String, Integer> backpackLimitCache;

    public PlayerManager(Main plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        reloadConfigValues();

        new BukkitRunnable() {
            @Override
            public void run() {
                checkLimit();
            }
        }.runTaskTimer(plugin, 20L * 5, 20L * 5);
    }

    public void reloadConfigValues() {
        FileConfiguration config = plugin.getConfig();
        int cacheExpireTime = config.getInt("limit-update-interval", 600);

        this.backpackLimitCache = CacheBuilder.newBuilder()
                .expireAfterWrite(cacheExpireTime, TimeUnit.SECONDS)
                .build(new CacheLoader<String, Integer>() {
                    public @NotNull Integer load(@NotNull String playerName) {
                        return databaseManager.getPlayerBackpackLimit(playerName);
                    }
                });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        updatePlayerLimit(player);
    }

    private void updatePlayerLimit(Player player) {
        int limit = backpackLimitCache.getUnchecked(player.getName());
        if (getOccupiedSlots(player.getInventory()) > limit) {
            removeExcessItems(player);
        }
        sendMessageWithCooldown(player, Messages.INVENTORY_LIMIT.replace("{limit}", String.valueOf(limit)));
    }

    private void removeExcessItems(Player player) {
        Inventory inventory = player.getInventory();
        int limit = backpackLimitCache.getUnchecked(player.getName());
        int occupiedSlots = getOccupiedSlots(inventory);

        if (occupiedSlots <= limit) return;

        Map<Material, Integer> removedItems = new HashMap<>();

        for (int i = inventory.getSize() - 1; i >= 0 && occupiedSlots > limit; i--) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                removedItems.put(item.getType(), removedItems.getOrDefault(item.getType(), 0) + item.getAmount());
                inventory.setItem(i, null);
                occupiedSlots--;
            }
        }

        for (Map.Entry<Material, Integer> entry : removedItems.entrySet()) {
            Material material = entry.getKey();
            int amount = entry.getValue();
            player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(material, amount));

            String message = Messages.INVENTORY_OVER_LIMIT_DROP
                    .replace("{amount}", String.valueOf(amount))
                    .replace("{item}", material.name().toLowerCase().replace("_", " "));

            sendMessageWithCooldown(player, message);
        }
    }

    public void checkLimit() {
        Bukkit.getOnlinePlayers().forEach(this::removeExcessItems);
    }

    private int getOccupiedSlots(Inventory inventory) {
        return (int) Stream.of(inventory.getContents())
                .filter(item -> item != null && item.getType() != Material.AIR)
                .count();
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        inventorySnapshot.put(player.getUniqueId(), getOccupiedSlots(player.getInventory()));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        int previousSlots = inventorySnapshot.getOrDefault(player.getUniqueId(), 0);
        int currentSlots = getOccupiedSlots(player.getInventory());
        int limit = backpackLimitCache.getUnchecked(player.getName());

        if (currentSlots > previousSlots && currentSlots > limit) {
            removeExcessItems(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPickupItemEvent(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        int limit = backpackLimitCache.getUnchecked(player.getName());
        int occupiedSlots = getOccupiedSlots(player.getInventory());

        ItemStack item = event.getItem().getItemStack();
        if (item.getType() == Material.AIR) return;

        for (ItemStack inventoryItem : player.getInventory().getContents()) {
            if (inventoryItem != null && inventoryItem.getType() == item.getType() &&
                    inventoryItem.getAmount() < inventoryItem.getMaxStackSize()) {
                return;
            }
        }

        if (occupiedSlots >= limit) {
            event.setCancelled(true);
            sendMessageWithCooldown(player, Messages.INVENTORY_OVER_LIMIT_PICKUP.replace("{item}", item.getType().name().toLowerCase()));
        }
    }

    private void sendMessageWithCooldown(Player player, String message) {
        if (message == null || message.isEmpty()) return;

        String playerName = player.getName();
        if (!cooldownMessages.contains(playerName)) {
            player.sendMessage(message.replace("{player}", player.getName()));
            cooldownMessages.add(playerName);

            Bukkit.getScheduler().runTaskLater(plugin, () -> cooldownMessages.remove(playerName), 100L);
        }
    }
}
