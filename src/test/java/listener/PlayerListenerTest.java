package listener;

import com.filizes.backpacklimit.Main;
import com.filizes.backpacklimit.config.service.MessageService;
import com.filizes.backpacklimit.config.settings.PerformanceSettings;
import com.filizes.backpacklimit.listener.PlayerListener;
import com.filizes.backpacklimit.listener.service.InventoryLimitService;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlayerListenerTest {

    @Mock private Main plugin;
    @Mock private InventoryLimitService limitService;
    @Mock private MessageService messageService;
    @Mock private PerformanceSettings performanceSettings;
    @Mock private Player player;
    @Mock private Item item;

    private PlayerListener playerListener;

    @BeforeEach
    void setUp() {
        when(performanceSettings.inventoryInteractionCheckDelayTicks()).thenReturn(2L);
        playerListener = new PlayerListener(plugin, limitService, messageService, performanceSettings);
    }

    @Test
    void onPlayerPickupItem_shouldCancelEvent_whenLimitExceeded() {
        ItemStack mockedItemStack = mock(ItemStack.class);
        when(mockedItemStack.getType()).thenReturn(Material.DIAMOND);

        when(item.getItemStack()).thenReturn(mockedItemStack);
        when(limitService.wouldExceedLimit(player, mockedItemStack)).thenReturn(true);
        when(limitService.formatItemName(mockedItemStack)).thenReturn("Diamond");
        EntityPickupItemEvent event = new EntityPickupItemEvent(player, item, 0);

        playerListener.onPlayerPickupItem(event);

        assertTrue(event.isCancelled());
        verify(messageService).sendThrottledMessage(eq(player), eq("inventory_over_limit_pickup"), any(Map.class));
    }

    @Test
    void onInventoryClick_shouldCancelEvent_whenMovingItemAndLimitExceeded() {
        ItemStack mockedItemStack = mock(ItemStack.class);
        when(mockedItemStack.getType()).thenReturn(Material.GOLD_INGOT);

        Inventory mockedChestInventory = mock(Inventory.class);

        when(limitService.isLimitApplicable(player)).thenReturn(true);
        when(limitService.wouldExceedLimit(player, mockedItemStack)).thenReturn(true);
        when(limitService.formatItemName(mockedItemStack)).thenReturn("Золотой слиток");

        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getAction()).thenReturn(InventoryAction.MOVE_TO_OTHER_INVENTORY);
        when(event.getClickedInventory()).thenReturn(mockedChestInventory);
        when(event.getCurrentItem()).thenReturn(mockedItemStack);

        playerListener.onInventoryClick(event);

        verify(event).setCancelled(true);
        verify(messageService).sendThrottledMessage(eq(player), eq("inventory_over_limit_pickup"), any(Map.class));
    }
}