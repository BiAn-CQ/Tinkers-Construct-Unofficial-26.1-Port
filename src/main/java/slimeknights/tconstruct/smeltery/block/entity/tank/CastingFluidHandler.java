package slimeknights.tconstruct.smeltery.block.entity.tank;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.tconstruct.library.utils.SimulationMode;
import slimeknights.tconstruct.library.utils.FluidStackDataUtil;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;

import java.util.Objects;

/** Transactional fluid handler for casting tables and basins. */
@RequiredArgsConstructor
public class CastingFluidHandler extends SnapshotJournal<CastingFluidHandler.State> implements ResourceHandler<FluidResource> {
  private final CastingBlockEntity tile;
  @Getter @Setter
  private FluidStack fluid = FluidStack.EMPTY;
  @Setter
  private int capacity = 0;
  private Fluid filter = Fluids.EMPTY;

  public FluidStack getFluid() {
    return fluid;
  }

  /** Checks if the given fluid is valid. */
  public boolean isFluidValid(FluidStack stack) {
    return !stack.isEmpty() && (filter == Fluids.EMPTY || stack.getFluid() == filter);
  }

  public boolean isEmpty() {
    return fluid.isEmpty();
  }

  /** Returns the recipe capacity, or the current amount before a recipe is selected. */
  public int getCapacity() {
    return capacity == 0 ? fluid.getAmount() : capacity;
  }

  /** Resets the tank and its recipe filter. */
  public void reset() {
    capacity = 0;
    fluid = FluidStack.EMPTY;
    filter = Fluids.EMPTY;
  }

  @Override
  public int size() {
    return 1;
  }

  @Override
  public FluidResource getResource(int index) {
    Objects.checkIndex(index, 1);
    return FluidResource.of(fluid);
  }

  @Override
  public long getAmountAsLong(int index) {
    Objects.checkIndex(index, 1);
    return fluid.getAmount();
  }

  @Override
  public long getCapacityAsLong(int index, FluidResource resource) {
    Objects.checkIndex(index, 1);
    return resource.isEmpty() || isValid(index, resource) ? getCapacity() : 0;
  }

  @Override
  public boolean isValid(int index, FluidResource resource) {
    return index == 0 && !resource.isEmpty() && (filter == Fluids.EMPTY || resource.getFluid() == filter);
  }

  @Override
  public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
    Objects.checkIndex(index, 1);
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    if (amount == 0 || !isValid(index, resource)) {
      return 0;
    }

    int targetCapacity = capacity;
    boolean initialize = targetCapacity == 0;
    if (initialize) {
      targetCapacity = tile.initNewCasting(resource.toStack(amount), SimulationMode.SIMULATE);
      if (targetCapacity <= 0) {
        return 0;
      }
    }

    if (!fluid.isEmpty() && !resource.matches(fluid)) {
      return 0;
    }
    int inserted = Math.min(amount, targetCapacity - fluid.getAmount());
    if (inserted <= 0) {
      return 0;
    }

    updateSnapshots(transaction);
    if (initialize) {
      capacity = targetCapacity;
      filter = resource.getFluid();
    }
    fluid = resource.toStack(fluid.getAmount() + inserted);
    return inserted;
  }

  @Override
  public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
    Objects.checkIndex(index, 1);
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    if (amount == 0 || fluid.isEmpty() || !resource.matches(fluid)) {
      return 0;
    }

    int extracted = Math.min(amount, fluid.getAmount());
    updateSnapshots(transaction);
    int remaining = fluid.getAmount() - extracted;
    fluid = remaining == 0 ? FluidStack.EMPTY : resource.toStack(remaining);
    return extracted;
  }

  @Override
  protected State createSnapshot() {
    return new State(fluid.copy(), capacity, filter);
  }

  @Override
  protected void revertToSnapshot(State snapshot) {
    fluid = snapshot.fluid().copy();
    capacity = snapshot.capacity();
    filter = snapshot.filter();
  }

  @Override
  protected void onRootCommit(State originalState) {
    boolean changed = originalState.capacity() != capacity
      || originalState.filter() != filter
      || originalState.fluid().getAmount() != fluid.getAmount()
      || (originalState.fluid().isEmpty() != fluid.isEmpty())
      || (!fluid.isEmpty() && !FluidStack.isSameFluidSameComponents(originalState.fluid(), fluid));
    if (!changed) {
      return;
    }

    if (fluid.isEmpty()) {
      tile.reset();
      return;
    }
    if (originalState.capacity() == 0 && capacity > 0) {
      int initializedCapacity = tile.initNewCasting(fluid, SimulationMode.EXECUTE);
      if (initializedCapacity != capacity) {
        throw new IllegalStateException("Casting recipe capacity changed during fluid transaction: expected "
          + capacity + ", got " + initializedCapacity);
      }
    }
    tile.onContentsChanged();
  }

  private static final String TAG_FLUID = "fluid";
  private static final String TAG_FILTER = "filter";
  private static final String TAG_CAPACITY = "capacity";

  public void readFromTag(CompoundTag nbt, HolderLookup.Provider provider) {
    capacity = nbt.getIntOr(TAG_CAPACITY, 0);
    fluid = nbt.contains(TAG_FLUID)
      ? FluidStackDataUtil.parse(provider, nbt.getCompoundOrEmpty(TAG_FLUID))
      : FluidStack.EMPTY;
    filter = Fluids.EMPTY;
    if (nbt.contains(TAG_FILTER)) {
      Fluid storedFilter = BuiltInRegistries.FLUID.getValue(Identifier.parse(nbt.getStringOr(TAG_FILTER, "")));
      if (storedFilter != null) {
        filter = storedFilter;
      }
    }
  }

  public void readFromTag(CompoundTag nbt) {
    if (tile.getLevel() != null) {
      readFromTag(nbt, tile.getLevel().registryAccess());
    }
  }

  public CompoundTag writeToTag(CompoundTag nbt, HolderLookup.Provider provider) {
    nbt.putInt(TAG_CAPACITY, capacity);
    if (!fluid.isEmpty()) {
      nbt.put(TAG_FLUID, FluidStackDataUtil.save(provider, fluid));
    }
    if (filter != Fluids.EMPTY) {
      nbt.putString(TAG_FILTER, BuiltInRegistries.FLUID.getKey(filter).toString());
    }
    return nbt;
  }

  public CompoundTag writeToTag(CompoundTag nbt) {
    return tile.getLevel() == null ? nbt : writeToTag(nbt, tile.getLevel().registryAccess());
  }

  protected record State(FluidStack fluid, int capacity, Fluid filter) {}
}
