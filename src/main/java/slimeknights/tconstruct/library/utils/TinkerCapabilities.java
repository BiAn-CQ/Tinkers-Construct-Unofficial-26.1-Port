package slimeknights.tconstruct.library.utils;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;

import javax.annotation.Nullable;

/** Small bridges from 26.1 transfer capabilities to legacy Tinkers handlers. */
public final class TinkerCapabilities {
  private TinkerCapabilities() {}

  @Nullable
  public static IItemHandler itemHandler(ItemStack stack) {
    if (stack.isEmpty()) {
      return null;
    }
    ResourceHandler<ItemResource> handler = stack.getCapability(Capabilities.Item.ITEM, ItemAccess.forStack(stack));
    return handler == null ? null : TinkerCapabilityAdapters.itemHandler(handler);
  }

  @Nullable
  public static IFluidHandlerItem fluidHandler(ItemStack stack) {
    return stack.isEmpty() ? null : FluidUtil.getFluidHandler(stack).orElse(null);
  }
}
