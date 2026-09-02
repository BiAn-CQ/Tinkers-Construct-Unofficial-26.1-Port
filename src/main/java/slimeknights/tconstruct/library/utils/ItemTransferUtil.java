package slimeknights.tconstruct.library.utils;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Native helpers for returning item stacks to players. */
public final class ItemTransferUtil {
  private ItemTransferUtil() {}

  public static void giveToPlayer(Player player, ItemStack stack) {
    player.getInventory().placeItemBackInInventory(stack);
  }

  public static void giveToPlayer(Player player, ItemStack stack, int preferredSlot) {
    if (stack.isEmpty()) {
      return;
    }
    Inventory inventory = player.getInventory();
    if (preferredSlot >= 0 && preferredSlot < inventory.getContainerSize()) {
      inventory.add(preferredSlot, stack);
    }
    if (!stack.isEmpty()) {
      inventory.placeItemBackInInventory(stack);
    }
  }
}
