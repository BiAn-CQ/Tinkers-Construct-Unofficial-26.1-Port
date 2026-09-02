package slimeknights.tconstruct.library.utils;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import javax.annotation.Nullable;

/** Helpers for querying native item and fluid capabilities from stacks. */
public final class TinkerCapabilities {
  private TinkerCapabilities() {}

  @Nullable
  public static ResourceHandler<ItemResource> itemHandler(ItemStack stack) {
    if (stack.isEmpty()) {
      return null;
    }
    return stack.getCapability(Capabilities.Item.ITEM, ItemAccess.forStack(stack));
  }

  @Nullable
  public static ResourceHandler<FluidResource> fluidHandler(ItemStack stack) {
    if (stack.isEmpty()) {
      return null;
    }
    return stack.getCapability(Capabilities.Fluid.ITEM, ItemAccess.forStack(stack));
  }
}
