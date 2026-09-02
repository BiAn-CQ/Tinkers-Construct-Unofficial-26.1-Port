package slimeknights.tconstruct.smeltery.block.entity.tank;

import com.google.common.collect.Lists;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.fluid.IMultitankListChange;
import slimeknights.tconstruct.library.utils.FluidStackDataUtil;
import slimeknights.tconstruct.library.utils.WeakListenerList;
import slimeknights.tconstruct.smeltery.block.entity.tank.ISmelteryTankHandler.FluidChange;
import slimeknights.tconstruct.smeltery.network.SmelteryTankUpdatePacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Transactional multi-fluid storage used by smelteries and foundries. */
public class SmelteryTank<T extends MantleBlockEntity & ISmelteryTankHandler> extends SnapshotJournal<SmelteryTank.State>
  implements ResourceHandler<FluidResource>, IMultitankListChange {
  private final T parent;
  @Getter
  private final List<FluidStack> fluids = Lists.newArrayList();
  private int capacity;
  @Getter
  private int contained;
  private final WeakListenerList tankListChange = new WeakListenerList();

  public SmelteryTank(T parent) {
    this.parent = parent;
  }

  public void syncFluids() {
    Level world = parent.getLevel();
    if (world != null && !world.isClientSide()) {
      BlockPos pos = parent.getBlockPos();
      TinkerNetwork.getInstance().sendToClientsAround(new SmelteryTankUpdatePacket(pos, fluids), world, pos);
    }
  }

  public void setCapacity(int maxCapacity) {
    capacity = maxCapacity;
  }

  public int getCapacity() {
    return capacity;
  }

  public int getRemainingSpace() {
    return Math.max(capacity - contained, 0);
  }

  @Override
  public int size() {
    return contained < capacity ? fluids.size() + 1 : fluids.size();
  }

  @Override
  public FluidResource getResource(int index) {
    Objects.checkIndex(index, size());
    return index < fluids.size() ? FluidResource.of(fluids.get(index)) : FluidResource.EMPTY;
  }

  @Override
  public long getAmountAsLong(int index) {
    Objects.checkIndex(index, size());
    return index < fluids.size() ? fluids.get(index).getAmount() : 0;
  }

  @Override
  public long getCapacityAsLong(int index, FluidResource resource) {
    Objects.checkIndex(index, size());
    int remaining = getRemainingSpace();
    return index < fluids.size() ? fluids.get(index).getAmount() + remaining : remaining;
  }

  @Override
  public boolean isValid(int index, FluidResource resource) {
    return index >= 0 && index < size() && !resource.isEmpty();
  }

  @Override
  public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
    Objects.checkIndex(index, size());
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    int remaining = getRemainingSpace();
    if (amount == 0 || remaining == 0) {
      return 0;
    }

    boolean adding = index == fluids.size();
    if (adding) {
      for (FluidStack stored : fluids) {
        if (resource.matches(stored)) {
          return 0;
        }
      }
    } else if (!resource.matches(fluids.get(index))) {
      return 0;
    }

    int inserted = Math.min(amount, remaining);
    updateSnapshots(transaction);
    contained += inserted;
    if (adding) {
      fluids.add(resource.toStack(inserted));
    } else {
      FluidStack stored = fluids.get(index);
      stored.grow(inserted);
    }
    return inserted;
  }

  @Override
  public int insert(FluidResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    for (int index = 0; index < fluids.size(); index++) {
      if (resource.matches(fluids.get(index))) {
        return insert(index, resource, amount, transaction);
      }
    }
    return contained < capacity ? insert(fluids.size(), resource, amount, transaction) : 0;
  }

  @Override
  public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
    Objects.checkIndex(index, size());
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    if (amount == 0 || index >= fluids.size() || !resource.matches(fluids.get(index))) {
      return 0;
    }

    FluidStack stored = fluids.get(index);
    int extracted = Math.min(amount, stored.getAmount());
    updateSnapshots(transaction);
    stored.shrink(extracted);
    contained -= extracted;
    if (stored.isEmpty()) {
      fluids.remove(index);
    }
    return extracted;
  }

  @Override
  public int extract(FluidResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    for (int index = 0; index < fluids.size(); index++) {
      if (resource.matches(fluids.get(index))) {
        return extract(index, resource, amount, transaction);
      }
    }
    return 0;
  }

  @Override
  protected State createSnapshot() {
    return new State(copyFluids(fluids), contained);
  }

  @Override
  protected void revertToSnapshot(State snapshot) {
    fluids.clear();
    fluids.addAll(copyFluids(snapshot.fluids()));
    contained = snapshot.contained();
  }

  @Override
  protected void onRootCommit(State originalState) {
    List<FluidStack> original = originalState.fluids();
    for (FluidStack current : fluids) {
      FluidStack previous = findMatching(original, current);
      if (previous == null) {
        parent.notifyFluidsChanged(FluidChange.ADDED, current);
      } else if (previous.getAmount() != current.getAmount()) {
        parent.notifyFluidsChanged(FluidChange.CHANGED, current);
      }
    }
    for (FluidStack previous : original) {
      if (findMatching(fluids, previous) == null) {
        parent.notifyFluidsChanged(FluidChange.REMOVED, previous);
      }
    }

    boolean listChanged = !sameResourceOrder(original, fluids);
    boolean fullStateChanged = (originalState.contained() >= capacity) != (contained >= capacity);
    if (listChanged || fullStateChanged) {
      tankListChange.run();
    }
  }

  public void moveFluidToBottom(int index) {
    if (index >= 0 && index < fluids.size()) {
      FluidStack fluid = fluids.remove(index);
      fluids.add(0, fluid);
      parent.notifyFluidsChanged(FluidChange.CHANGED, FluidStack.EMPTY);
      tankListChange.run();
    }
  }

  private static final String TAG_FLUIDS = "fluids";
  private static final String TAG_CAPACITY = "capacity";

  public void setFluids(List<FluidStack> updatedFluids) {
    FluidStack oldFirst = fluids.isEmpty() ? FluidStack.EMPTY : fluids.getFirst();
    fluids.clear();
    fluids.addAll(updatedFluids);
    contained = fluids.stream().mapToInt(FluidStack::getAmount).sum();
    FluidStack newFirst = fluids.isEmpty() ? FluidStack.EMPTY : fluids.getFirst();
    if (!FluidStack.isSameFluidSameComponents(oldFirst, newFirst)) {
      parent.notifyFluidsChanged(FluidChange.ORDER_CHANGED, newFirst);
      tankListChange.run();
    }
  }

  public CompoundTag write(CompoundTag nbt, HolderLookup.Provider provider) {
    ListTag list = new ListTag();
    for (FluidStack liquid : fluids) {
      list.add(FluidStackDataUtil.save(provider, liquid));
    }
    nbt.put(TAG_FLUIDS, list);
    nbt.putInt(TAG_CAPACITY, capacity);
    return nbt;
  }

  public CompoundTag write(CompoundTag nbt) {
    return parent.getLevel() == null ? nbt : write(nbt, parent.getLevel().registryAccess());
  }

  public void read(CompoundTag tag, HolderLookup.Provider provider) {
    ListTag list = tag.getListOrEmpty(TAG_FLUIDS);
    fluids.clear();
    contained = 0;
    for (int i = 0; i < list.size(); i++) {
      FluidStack fluid = FluidStackDataUtil.parse(provider, list.getCompoundOrEmpty(i));
      if (!fluid.isEmpty()) {
        fluids.add(fluid);
        contained += fluid.getAmount();
      }
    }
    capacity = tag.getIntOr(TAG_CAPACITY, 0);
  }

  public void read(CompoundTag tag) {
    if (parent.getLevel() != null) {
      read(tag, parent.getLevel().registryAccess());
    }
  }

  @Override
  public <TE> void addTankListListener(TE parent, Consumer<TE> listener) {
    tankListChange.addListener(parent, listener);
  }

  @Override
  public void removeTankListListeners(Object parent) {
    tankListChange.removeListeners(parent);
  }

  private static List<FluidStack> copyFluids(List<FluidStack> source) {
    List<FluidStack> copy = new ArrayList<>(source.size());
    for (FluidStack fluid : source) {
      copy.add(fluid.copy());
    }
    return copy;
  }

  private static FluidStack findMatching(List<FluidStack> source, FluidStack target) {
    for (FluidStack fluid : source) {
      if (FluidStack.isSameFluidSameComponents(fluid, target)) {
        return fluid;
      }
    }
    return null;
  }

  private static boolean sameResourceOrder(List<FluidStack> first, List<FluidStack> second) {
    if (first.size() != second.size()) {
      return false;
    }
    for (int i = 0; i < first.size(); i++) {
      if (!FluidStack.isSameFluidSameComponents(first.get(i), second.get(i))) {
        return false;
      }
    }
    return true;
  }

  protected record State(List<FluidStack> fluids, int contained) {}
}
