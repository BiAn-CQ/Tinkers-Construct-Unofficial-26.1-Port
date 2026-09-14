package slimeknights.tconstruct.library.utils;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class ItemTransferUtilTest extends slimeknights.tconstruct.test.CoreTestBootstrap {
  @Test
  void incompatibleHeldItemDoesNotConsumeOrConvertOutput() {
    for (boolean creative : new boolean[]{false, true}) {
      Player player = mock(Player.class);
      Inventory inventory = mock(Inventory.class);
      when(player.getInventory()).thenReturn(inventory);
      when(player.hasInfiniteMaterials()).thenReturn(creative);
      when(inventory.getContainerSize()).thenReturn(36);
      ItemStack held = new ItemStack(Items.DIRT, 4);
      ItemStack output = new ItemStack(Items.IRON_BLOCK);
      when(inventory.getItem(0)).thenReturn(held);
      doAnswer(invocation -> {
        ItemStack received = invocation.getArgument(0);
        assertThat(received.getItem()).isSameAs(Items.IRON_BLOCK);
        assertThat(received.getCount()).isEqualTo(1);
        received.setCount(0);
        return null;
      }).when(inventory).placeItemBackInInventory(any(ItemStack.class));

      ItemTransferUtil.giveToPlayer(player, output, 0);

      assertThat(held.getCount()).isEqualTo(4);
      assertThat(output.getCount()).isEqualTo(1);
      verify(inventory).placeItemBackInInventory(any(ItemStack.class));
      verify(inventory, never()).add(anyInt(), any(ItemStack.class));
    }
  }

  @Test
  void matchingHeldStackOnlyReceivesAvailableSpace() {
    Player player = mock(Player.class);
    Inventory inventory = mock(Inventory.class);
    when(player.getInventory()).thenReturn(inventory);
    when(inventory.getContainerSize()).thenReturn(36);
    ItemStack held = new ItemStack(Items.IRON_BLOCK);
    held.set(DataComponents.MAX_STACK_SIZE, 64);
    int limit = held.getMaxStackSize();
    held.setCount(limit - 1);
    ItemStack output = new ItemStack(Items.IRON_BLOCK, 3);
    output.set(DataComponents.MAX_STACK_SIZE, 64);
    when(inventory.getItem(0)).thenReturn(held);
    doAnswer(invocation -> {
      ItemStack remainder = invocation.getArgument(0);
      assertThat(remainder.getCount()).isEqualTo(2);
      return null;
    }).when(inventory).placeItemBackInInventory(any(ItemStack.class));

    ItemTransferUtil.giveToPlayer(player, output, 0);

    assertThat(held.getCount()).isEqualTo(limit);
    assertThat(output.getCount()).isEqualTo(3);
    verify(inventory).placeItemBackInInventory(any(ItemStack.class));
  }
}
