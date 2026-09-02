package slimeknights.tconstruct.smeltery.item;

import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.ItemAccessFluidHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.library.recipe.FluidValues;

/** Native all-or-nothing fluid handler for copper cans. */
public class CopperCanFluidHandler extends ItemAccessFluidHandler {
  public CopperCanFluidHandler(ItemAccess itemAccess) {
    super(itemAccess, TinkerModule.FLUID_STACK_COMPONENT.get(), FluidValues.INGOT);
  }

  @Override
  public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
    int capacity = getCapacityAsInt(index, resource);
    return getAmountAsInt(index) == 0 && amount >= capacity
      ? super.insert(index, resource, capacity, transaction)
      : 0;
  }

  @Override
  public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
    int stored = getAmountAsInt(index);
    return stored > 0 && amount >= stored
      ? super.extract(index, resource, stored, transaction)
      : 0;
  }
}
