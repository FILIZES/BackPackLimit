package com.filizes.backpacklimit.listener.service;

import com.filizes.backpacklimit.config.service.MessageService;
import com.filizes.backpacklimit.config.settings.LimitSettings;
import com.filizes.backpacklimit.database.interfaces.DatabaseManager;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
public final class InventoryLimitService {

    final DatabaseManager databaseManager;
    final MessageService messageService;
    final AsyncLoadingCache<UUID, Integer> backpackLimitCache;
    final Executor mainThreadExecutor;

    boolean ignoreArmorAndOffhand;
    int defaultLimit;

    @Inject
    public InventoryLimitService(
            DatabaseManager databaseManager,
            MessageService messageService,
            LimitSettings limitSettings,
            AsyncLoadingCache<UUID, Integer> backpackLimitCache,
            @Named("bukkitMainThreadExecutor") Executor mainThreadExecutor
    ) {
        this.databaseManager = databaseManager;
        this.messageService = messageService;
        this.backpackLimitCache = backpackLimitCache;
        this.mainThreadExecutor = mainThreadExecutor;
        this.reload(limitSettings);
    }

    public void reload(LimitSettings newSettings) {
        this.defaultLimit = newSettings.defaultLimit();
        this.ignoreArmorAndOffhand = newSettings.ignoreArmorAndOffhand();
    }

    public void performScheduledCheck() {
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.hasMetadata("NPC"))
                .forEach(this::checkAndCorrectInventoryAsync);
    }

    public void handlePlayerJoin(Player player) {
        databaseManager.ensurePlayerExists(player);
        getLimit(player.getUniqueId()).thenAcceptAsync(limit -> {
            if (player.isOnline()) {
                checkAndCorrectInventory(player, limit);
                messageService.sendMessage(player, "inventory_limit", Map.of("{limit}", String.valueOf(limit)));
            }
        }, mainThreadExecutor);
    }

    public void handlePlayerQuit(Player player) {
        backpackLimitCache.synchronous().invalidate(player.getUniqueId());
    }

    public boolean isLimitApplicable(Player player) {
        return !player.hasPermission("backpacklimit.bypass") && !player.hasMetadata("NPC");
    }

    public void checkAndCorrectInventoryAsync(Player player) {
        if (!player.isOnline() || player.hasPermission("backpacklimit.bypass")) {
            return;
        }
        getLimit(player.getUniqueId()).thenAcceptAsync(limit ->
                checkAndCorrectInventory(player, limit), mainThreadExecutor
        );
    }

    private void checkAndCorrectInventory(Player player, int limit) {
        if (!player.isOnline()) return;

        PlayerInventory inventory = player.getInventory();
        List<Integer> occupiedSlots = getOccupiedSlots(inventory);

        if (occupiedSlots.size() <= limit) {
            return;
        }

        int countToRemove = occupiedSlots.size() - limit;
        List<Integer> slotsToClear = occupiedSlots.stream().limit(countToRemove).toList();

        dropItemsFromSlots(player, slotsToClear);
    }

    private void dropItemsFromSlots(Player player, List<Integer> slotsToClear) {
        PlayerInventory inventory = player.getInventory();
        List<ItemStack> itemsToDrop = new ArrayList<>();

        for (int slot : slotsToClear) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                itemsToDrop.add(item.clone());
                inventory.setItem(slot, null);
            }
        }

        if (!itemsToDrop.isEmpty()) {
            itemsToDrop.forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));

            var removedSummary = itemsToDrop.stream()
                    .collect(Collectors.groupingBy(this::formatItemName, Collectors.summingInt(ItemStack::getAmount)));

            removedSummary.forEach((itemName, amount) ->
                    messageService.sendThrottledMessage(player, "inventory_over_limit_drop", Map.of(
                            "{item}", itemName,
                            "{amount}", String.valueOf(amount)
                    ))
            );
            player.updateInventory();
        }
    }

    private List<Integer> getOccupiedSlots(PlayerInventory inventory) {
        ItemStack[] contents = ignoreArmorAndOffhand ? inventory.getStorageContents() : inventory.getContents();
        return IntStream.range(0, contents.length)
                .filter(i -> contents[i] != null && !contents[i].getType().isAir())
                .boxed()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    public boolean wouldExceedLimit(Player player, ItemStack itemToPickup) {
        if (!isLimitApplicable(player)) {
            return false;
        }

        int limit = getCachedLimit(player.getUniqueId());
        PlayerInventory inventory = player.getInventory();

        long occupiedSlots = getOccupiedSlots(inventory).size();

        if (occupiedSlots < limit) {
            return false;
        }

        int spaceAvailableInStacks = 0;
        ItemStack[] contents = ignoreArmorAndOffhand ? inventory.getStorageContents() : inventory.getContents();
        for (ItemStack existingItem : contents) {
            if (existingItem != null && existingItem.isSimilar(itemToPickup)) {
                spaceAvailableInStacks += existingItem.getMaxStackSize() - existingItem.getAmount();
            }
        }

        return spaceAvailableInStacks < itemToPickup.getAmount();
    }

    public int getCachedLimit(UUID playerUUID) {
        return Optional.ofNullable(backpackLimitCache.getIfPresent(playerUUID))
                .map(future -> future.getNow(defaultLimit))
                .orElse(defaultLimit);
    }

    public CompletableFuture<Integer> getLimit(UUID playerUUID) {
        return backpackLimitCache.get(playerUUID);
    }

    public void updatePlayerLimitInCache(UUID playerUUID, int newLimit) {
        backpackLimitCache.put(playerUUID, CompletableFuture.completedFuture(newLimit));
    }

    public String formatItemName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }

        String name = item.getType().name().replace('_', ' ').toLowerCase();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}