package inventory;

import com.filizes.backpacklimit.config.service.MessageService;
import com.filizes.backpacklimit.config.settings.LimitSettings;
import com.filizes.backpacklimit.database.interfaces.DatabaseManager;
import com.filizes.backpacklimit.listener.service.InventoryLimitService;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryLimitServiceTest {

    @Mock private DatabaseManager databaseManager;
    @Mock private MessageService messageService;
    @Mock private Player player;
    @Mock private PlayerInventory inventory;
    @Mock private LimitSettings limitSettings;
    @Mock private AsyncLoadingCache<UUID, Integer> backpackLimitCache;
    @Mock private Executor mainThreadExecutor;

    private InventoryLimitService inventoryLimitService;
    private final UUID playerUUID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(limitSettings.defaultLimit()).thenReturn(5);
        when(limitSettings.ignoreArmorAndOffhand()).thenReturn(true);

        inventoryLimitService = new InventoryLimitService(
                databaseManager,
                messageService,
                limitSettings,
                backpackLimitCache,
                mainThreadExecutor
        );

        when(player.getUniqueId()).thenReturn(playerUUID);
        when(player.getInventory()).thenReturn(inventory);

        when(backpackLimitCache.get(any(UUID.class))).thenReturn(CompletableFuture.completedFuture(5));
    }

    private ItemStack createMockItem(Material material, int amount) {
        ItemStack mockItem = mock(ItemStack.class);
        when(mockItem.getType()).thenReturn(material);
        when(mockItem.getAmount()).thenReturn(amount);
        return mockItem;
    }

    @Test
    void wouldExceedLimit_shouldReturnFalse_whenPlayerHasBypassPermission() {
        when(player.hasPermission("backpacklimit.bypass")).thenReturn(true);
        assertFalse(inventoryLimitService.wouldExceedLimit(player, mock(ItemStack.class)));
    }

    @Test
    void wouldExceedLimit_shouldReturnTrue_whenInventoryFullAndNoSpaceInStacks() {
        ItemStack[] storageContents = new ItemStack[36];
        for (int i = 0; i < 5; i++) {
            storageContents[i] = createMockItem(Material.STONE, 1);
        }

        when(inventory.getStorageContents()).thenReturn(storageContents);

        inventoryLimitService.getLimit(playerUUID).join();

        ItemStack itemToPickup = createMockItem(Material.DIAMOND, 1);

        for (ItemStack itemInInventory : storageContents) {
            if (itemInInventory != null) {
                when(itemInInventory.isSimilar(itemToPickup)).thenReturn(false);
            }
        }
        assertTrue(inventoryLimitService.wouldExceedLimit(player, itemToPickup));
    }

    @Test
    void wouldExceedLimit_shouldReturnFalse_whenInventoryFullButCanStack() {
        ItemStack[] storageContents = new ItemStack[36];
        for (int i = 0; i < 4; i++) {
            storageContents[i] = createMockItem(Material.DIRT, 1);
        }
        ItemStack stackableItemInInventory = createMockItem(Material.STONE, 32);
        storageContents[4] = stackableItemInInventory;

        when(inventory.getStorageContents()).thenReturn(storageContents);

        inventoryLimitService.getLimit(playerUUID).join();

        ItemStack itemToPickup = createMockItem(Material.STONE, 16);
        when(stackableItemInInventory.isSimilar(itemToPickup)).thenReturn(true);
        when(stackableItemInInventory.getMaxStackSize()).thenReturn(64);

        assertFalse(inventoryLimitService.wouldExceedLimit(player, itemToPickup));
    }
}