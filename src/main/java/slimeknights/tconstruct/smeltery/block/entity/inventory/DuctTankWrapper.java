package slimeknights.tconstruct.smeltery.block.entity.inventory;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.tconstruct.library.fluid.IMultitankListChange;

import java.util.function.Consumer;

/** Native filtered view over a smeltery tank. */
public class DuctTankWrapper implements ResourceHandler<FluidResource> {
  private final ResourceHandler<FluidResource> parent;
  private final DuctItemHandler itemHandler;
  private int[] tankMapping;

  public DuctTankWrapper(ResourceHandler<FluidResource> parent, DuctItemHandler itemHandler) {
    this.parent = parent;
    this.itemHandler = itemHandler;
    Consumer<DuctTankWrapper> consumer = self -> self.tankMapping = null;
    itemHandler.addListener(this, consumer);
    if (parent instanceof IMultitankListChange notifier) {
      notifier.addTankListListener(this, consumer);
    }
  }

  private int[] getTankMapping() {
    if (tankMapping == null) {
      FluidResource filter = FluidResource.of(itemHandler.getFluid());
      int count = parent.size();
      if (count <= 0) {
        tankMapping = new int[0];
      } else if (filter.isEmpty()) {
        tankMapping = parent.getResource(count - 1).isEmpty() ? new int[] { count - 1 } : new int[0];
      } else {
        IntList list = new IntArrayList(count);
        for (int i = 0; i < count; i++) {
          FluidResource contained = parent.getResource(i);
          if (contained.isEmpty() || contained.equals(filter)) {
            list.add(i);
          }
        }
        tankMapping = list.toIntArray();
      }
    }
    return tankMapping;
  }

  private int getMappedTank(int tank) {
    int[] mapping = getTankMapping();
    return tank >= 0 && tank < mapping.length ? mapping[tank] : -1;
  }

  @Override
  public int size() {
    return getTankMapping().length;
  }

  @Override
  public FluidResource getResource(int index) {
    return parent.getResource(getTankMapping()[index]);
  }

  @Override
  public long getAmountAsLong(int index) {
    return parent.getAmountAsLong(getTankMapping()[index]);
  }

  @Override
  public long getCapacityAsLong(int index, FluidResource resource) {
    return parent.getCapacityAsLong(getTankMapping()[index], resource);
  }

  @Override
  public boolean isValid(int index, FluidResource resource) {
    FluidStack filter = itemHandler.getFluid();
    int mapped = getMappedTank(index);
    return mapped >= 0 && !filter.isEmpty() && resource.matches(filter) && parent.isValid(mapped, resource);
  }

  @Override
  public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    int mapped = getMappedTank(index);
    return mapped >= 0 && resource.matches(itemHandler.getFluid())
      ? parent.insert(mapped, resource, amount, transaction) : 0;
  }

  @Override
  public int insert(FluidResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    return size() == 0 ? 0 : insert(0, resource, amount, transaction);
  }

  @Override
  public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    int mapped = getMappedTank(index);
    return mapped >= 0 && resource.matches(itemHandler.getFluid())
      ? parent.extract(mapped, resource, amount, transaction) : 0;
  }

  @Override
  public int extract(FluidResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    for (int index = 0; index < size(); index++) {
      if (resource.equals(getResource(index))) {
        return extract(index, resource, amount, transaction);
      }
    }
    return 0;
  }
}
