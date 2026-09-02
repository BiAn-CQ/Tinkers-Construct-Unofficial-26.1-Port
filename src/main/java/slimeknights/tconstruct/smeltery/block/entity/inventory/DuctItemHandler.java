package slimeknights.tconstruct.smeltery.block.entity.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import slimeknights.mantle.inventory.SingleItemHandler;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.network.InventorySlotSyncPacket;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.utils.WeakListenerList;
import slimeknights.tconstruct.smeltery.block.entity.component.DuctBlockEntity;

import java.util.function.Consumer;

/**
 * Item handler for the duct
 */
public class DuctItemHandler extends SingleItemHandler<DuctBlockEntity> {
  private final WeakListenerList onUpdate = new WeakListenerList();
  private FluidStack fluid = null;
  public DuctItemHandler(DuctBlockEntity parent) {
    super(parent, 1);
  }

  /** Called when the fluid changes to alert listeners */
  private void updateFluid() {
    fluid = null;
    onUpdate.run();
  }

  /**
   * Adds a listener to run when the fluid updates.
   * Generally these should just clear cache rather than immediately consuming the new fluid.
   */
  public <T> void addListener(T parent, Consumer<T> listener) {
    onUpdate.addListener(parent, listener);
  }

  /**
   * Sets the stack in this duct
   * @param newStack  New stack
   */
  @Override
  protected void onStackChanged(ItemStack current, ItemStack newStack) {
    Level world = parent.getLevel();
    // if both are empty, assume shift click so we need to update
    boolean hasChange = (current.isEmpty() && newStack.isEmpty()) || !ItemStack.matches(current, newStack);
    if (hasChange) {
      updateFluid();
      if (world != null) {
        if (!world.isClientSide()) {
          BlockPos pos = parent.getBlockPos();
          TinkerNetwork.getInstance().sendToClientsAround(new InventorySlotSyncPacket(newStack, 0, pos), world, pos);
        } else {
          parent.updateFluid();
        }
      }
    }
  }

  @Override
  protected boolean isItemValid(ItemStack stack) {
    // the item or its container must be in the tag
    if (!stack.is(TinkerTags.Items.DUCT_CONTAINERS)) {
      var remainder = stack.getCraftingRemainder();
      ItemStack container = remainder == null ? ItemStack.EMPTY : remainder.create();
      if (container.isEmpty() || !container.is(TinkerTags.Items.DUCT_CONTAINERS)) {
        return false;
      }
    }
    // the item must contain fluid (no empty cans or buckets)
    var capability = slimeknights.tconstruct.library.utils.TinkerCapabilities.fluidHandler(stack);
    return capability != null && capability.size() > 0 && !capability.getResource(0).isEmpty();
  }

  /**
   * Gets the fluid filter for this duct
   * @return  Fluid filter
   */
  public FluidStack getFluid() {
    if (fluid == null) {
      ItemStack stack = getStack();
      if (stack.isEmpty()) {
        fluid = FluidStack.EMPTY;
      } else {
        var handler = slimeknights.tconstruct.library.utils.TinkerCapabilities.fluidHandler(stack);
        fluid = handler == null || handler.size() == 0 ? FluidStack.EMPTY : FluidUtil.getStack(handler, 0);
      }
    }
    return fluid;
  }
}
