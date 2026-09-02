package slimeknights.tconstruct.smeltery.item;

import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.ItemAccessFluidHandler;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity;

/**
 * Native fluid resource handler backed by a tank item stack.
 */
public class TankItemFluidHandler extends ItemAccessFluidHandler {
  private final TankItem tankItem;

  public TankItemFluidHandler(TankItem tankItem, ItemAccess itemAccess) {
    super(itemAccess, TinkerModule.FLUID_STACK_COMPONENT.get(),
      TankBlockEntity.getCapacity(itemAccess.getResource().getItem()));
    this.tankItem = tankItem;
  }

  @Override
  public boolean isValid(int index, FluidResource resource) {
    return super.isValid(index, resource) && tankItem.canFill(itemAccess.getAmount());
  }
}
