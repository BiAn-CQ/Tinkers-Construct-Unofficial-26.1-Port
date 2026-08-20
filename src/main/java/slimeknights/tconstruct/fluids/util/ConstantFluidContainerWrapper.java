package slimeknights.tconstruct.fluids.util;

import net.minecraft.world.item.ItemStackTemplate;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/** Resource handler for a container with a fixed fluid payload. */
public class ConstantFluidContainerWrapper implements ResourceHandler<FluidResource> {
  private final ItemAccess itemAccess;
  private final ItemResource filledItem;
  private final FluidResource fluid;
  private final int amount;
  @Nullable
  private final ItemResource emptyItem;

  public ConstantFluidContainerWrapper(FluidStack fluid, ItemAccess itemAccess) {
    this(fluid, itemAccess, null);
  }

  public ConstantFluidContainerWrapper(FluidStack fluid, ItemAccess itemAccess, @Nullable ItemResource emptyItem) {
    this.itemAccess = itemAccess;
    this.filledItem = itemAccess.getResource();
    this.fluid = FluidResource.of(fluid);
    this.amount = fluid.getAmount();
    if (emptyItem != null) {
      this.emptyItem = emptyItem;
    } else {
      ItemStackTemplate remainder = filledItem.toStack().getCraftingRemainder();
      this.emptyItem = remainder == null ? null : ItemResource.of(remainder.create());
    }
  }

  private boolean isFilled() {
    return itemAccess.getAmount() > 0 && itemAccess.getResource().equals(filledItem);
  }

  @Override
  public int size() {
    return 1;
  }

  @Override
  public FluidResource getResource(int index) {
    return index == 0 && isFilled() ? fluid : FluidResource.EMPTY;
  }

  @Override
  public long getAmountAsLong(int index) {
    return index == 0 && isFilled() ? amount : 0;
  }

  @Override
  public long getCapacityAsLong(int index, FluidResource resource) {
    return index == 0 && (resource.isEmpty() || resource.equals(fluid)) ? amount : 0;
  }

  @Override
  public boolean isValid(int index, FluidResource resource) {
    return index == 0 && resource.equals(fluid);
  }

  @Override
  public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    return 0;
  }

  @Override
  public int extract(int index, FluidResource resource, int requested, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, requested);
    if (index != 0 || !isFilled() || !resource.equals(fluid) || requested < amount) {
      return 0;
    }
    int changed = emptyItem == null
      ? itemAccess.extract(filledItem, 1, transaction)
      : itemAccess.exchange(emptyItem, 1, transaction);
    return changed == 1 ? amount : 0;
  }
}
