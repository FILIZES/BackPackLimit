package com.filizes.backpacklimit.listener;

import com.filizes.backpacklimit.Main;
import com.filizes.backpacklimit.config.service.MessageService;
import com.filizes.backpacklimit.config.settings.PerformanceSettings;
import com.filizes.backpacklimit.listener.service.InventoryLimitService;
import com.google.inject.Inject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;

public final class PlayerListener implements Listener {

    private final Main plugin;
    private final InventoryLimitService limitService;
    private final MessageService messageService;
    private final long inventoryCheckDelay;

    @Inject
    public PlayerListener(
            Main plugin,
            InventoryLimitService limitService,
            MessageService messageService,
            PerformanceSettings performanceSettings
    ) {
        this.plugin = plugin;
        this.limitService = limitService;
        this.messageService = messageService;
        this.inventoryCheckDelay = performanceSettings.inventoryInteractionCheckDelayTicks();
    }
    @EventHandler public void onPlayerJoin(PlayerJoinEvent event) {
        limitService.handlePlayerJoin(event.getPlayer());
    }

    @EventHandler public void onPlayerQuit(PlayerQuitEvent event) {
        limitService.handlePlayerQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            scheduleInventoryCheck(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (limitService.wouldExceedLimit(player, event.getItem().getItemStack())) {
                event.setCancelled(true);
                messageService.sendThrottledMessage(player, "inventory_over_limit_pickup", Map.of(
                        "{item}", limitService.formatItemName(event.getItem().getItemStack())
                ));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !limitService.isLimitApplicable(player)) {
            return;
        }

        if (!isItemMovingIntoPlayerInventory(event)) {
            return;
        }

        ItemStack movedItem = getItemFromAction(event);
        if (movedItem == null || movedItem.getType().isAir()) {
            return;
        }

        if (limitService.wouldExceedLimit(player, movedItem)) {
            cancelAndNotify(event, player, movedItem);
        }
    }

    private boolean isItemMovingIntoPlayerInventory(InventoryClickEvent event) {
        Inventory clickedInv = event.getClickedInventory();
        InventoryAction action = event.getAction();

        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            return !(clickedInv instanceof PlayerInventory);
        }

        if (clickedInv instanceof PlayerInventory) {
            return switch (action) {
                case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> true;
                default -> false;
            };
        }

        return false;
    }

    private ItemStack getItemFromAction(InventoryClickEvent event) {
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            return event.getCurrentItem();
        }
        return event.getCursor();
    }

    private void cancelAndNotify(InventoryClickEvent event, Player player, ItemStack item) {
        event.setCancelled(true);
        messageService.sendThrottledMessage(player, "inventory_over_limit_pickup", Map.of(
                "{item}", limitService.formatItemName(item)
        ));
    }

    private void scheduleInventoryCheck(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                limitService.checkAndCorrectInventoryAsync(player);
            }
        }, inventoryCheckDelay);
    }
}