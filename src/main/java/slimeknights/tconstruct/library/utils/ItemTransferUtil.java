package slimeknights.tconstruct.library.utils;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Native helpers for returning item stacks to players. */
public final class ItemTransferUtil {
  private ItemTransferUtil() {}

  public static void giveToPlayer(Player player, ItemStack stack) {
    player.getInventory().placeItemBackInInventory(stack.copy());
  }

  public static void giveToPlayer(Player player, ItemStack stack, int preferredSlot) {
    if (stack.isEmpty()) {
      return;
    }
    Inventory inventory = player.getInventory();
    // Keep the caller's container stack intact until its setter sends the removal update.
    stack = stack.copy();
    if (preferredSlot >= 0 && preferredSlot < inventory.getContainerSize()) {
      ItemStack existing = inventory.getItem(preferredSlot);
      if (existing.isEmpty()) {
        inventory.setItem(preferredSlot, stack.split(stack.getMaxStackSize()));
      } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
        int moved = Math.min(stack.getCount(), Math.max(0, existing.getMaxStackSize() - existing.getCount()));
        existing.grow(moved);
        stack.shrink(moved);
      }
    }
    if (!stack.isEmpty()) {
      inventory.placeItemBackInInventory(stack);
    }
  }
}
