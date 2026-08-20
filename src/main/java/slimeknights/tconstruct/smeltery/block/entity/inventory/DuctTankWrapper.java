package slimeknights.tconstruct.smeltery.block.entity.inventory;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import slimeknights.tconstruct.library.fluid.IMultitankListChange;
import slimeknights.tconstruct.library.fluid.IndexedFluidHandler;

import java.util.function.Consumer;

public class DuctTankWrapper implements IndexedFluidHandler {
  private final IFluidHandler parent;
  private final DuctItemHandler itemHandler;
  private int[] tankMapping;

  public DuctTankWrapper(IFluidHandler parent, DuctItemHandler itemHandler) {
    this.parent = parent;
    this.itemHandler = itemHandler;
    // clear cache when the fluid changes or the smeltery list changes
    Consumer<DuctTankWrapper> consumer = self -> self.tankMapping = null;
    itemHandler.addListener(this, consumer);
    if (parent instanceof IMultitankListChange notifier) {
      notifier.addTankListListener(this, consumer);
    }
  }

  /** Gets the mapping from index to matching tank */
  private int[] getTankMapping() {
    if (tankMapping == null) {
      FluidStack filter = itemHandler.getFluid();
      int count = parent.getTanks();
      if (count <= 0) {
        tankMapping = new int[0];
        return tankMapping;
      }
      if (filter.isEmpty()) {
        FluidStack last = parent.getFluidInTank(count - 1);
        if (last.isEmpty()) {
          tankMapping = new int[] { count - 1 };
        } else {
          tankMapping = new int[0];
        }
      } else {
        IntList list = new IntArrayList(count);
        for (int i = 0; i < count; i++) {
          FluidStack contained = parent.getFluidInTank(i);
          if (contained.isEmpty() || FluidStack.isSameFluidSameComponents(filter, contained)) {
            list.add(i);
          }
        }
        tankMapping = list.toIntArray();
      }
    }
    return tankMapping;
  }


  /* Properties */

  @Override
  public int getTanks() {
    return getTankMapping().length;
  }

  @Override
  public FluidStack getFluidInTank(int tank) {
    if (tank < 0) {
      return FluidStack.EMPTY;
    }
    int[] mapping = getTankMapping();
    if (tank >= mapping.length) {
      return FluidStack.EMPTY;
    }
    return parent.getFluidInTank(mapping[tank]);
  }

  @Override
  public int getTankCapacity(int tank) {
    if (tank < 0) {
      return 0;
    }
    int[] mapping = getTankMapping();
    if (tank >= mapping.length) {
      return 0;
    }
    return parent.getTankCapacity(mapping[tank]);
  }

  @Override
  public boolean isFluidValid(int tank, FluidStack stack) {
    return FluidStack.isSameFluidSameComponents(itemHandler.getFluid(), stack);
  }


  /* Interactions */

  @Override
  public int fill(FluidStack resource, FluidAction action) {
    if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(itemHandler.getFluid(), resource)) {
      return 0;
    }
    return parent.fill(resource, action);
  }

  @Override
  public int fill(int tank, FluidStack resource, FluidAction action) {
    if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(itemHandler.getFluid(), resource)) {
      return 0;
    }
    int mapped = getMappedTank(tank);
    if (mapped < 0) {
      return 0;
    }
    if (parent instanceof IndexedFluidHandler indexed) {
      return indexed.fill(mapped, resource, action);
    }
    return mapped == 0 && parent.getTanks() == 1 ? parent.fill(resource, action) : 0;
  }

  @Override
  public FluidStack drain(int maxDrain, FluidAction action) {
    FluidStack fluid = itemHandler.getFluid();
    if (fluid.isEmpty()) {
      return FluidStack.EMPTY;
    }
    return parent.drain(fluid.copyWithAmount(maxDrain), action);
  }

  @Override
  public FluidStack drain(FluidStack resource, FluidAction action) {
    if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(itemHandler.getFluid(), resource)) {
      return FluidStack.EMPTY;
    }
    return parent.drain(resource, action);
  }

  @Override
  public FluidStack drain(int tank, FluidStack resource, FluidAction action) {
    if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(itemHandler.getFluid(), resource)) {
      return FluidStack.EMPTY;
    }
    int mapped = getMappedTank(tank);
    if (mapped < 0) {
      return FluidStack.EMPTY;
    }
    if (parent instanceof IndexedFluidHandler indexed) {
      return indexed.drain(mapped, resource, action);
    }
    return mapped == 0 && parent.getTanks() == 1 ? parent.drain(resource, action) : FluidStack.EMPTY;
  }

  private int getMappedTank(int tank) {
    int[] mapping = getTankMapping();
    return tank >= 0 && tank < mapping.length ? mapping[tank] : -1;
  }
}
