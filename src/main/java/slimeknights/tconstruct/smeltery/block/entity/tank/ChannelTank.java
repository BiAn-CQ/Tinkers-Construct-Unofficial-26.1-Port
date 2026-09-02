package slimeknights.tconstruct.smeltery.block.entity.tank;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.tconstruct.library.fluid.FluidTankBase;
import slimeknights.tconstruct.smeltery.block.entity.ChannelBlockEntity;

/** Transactional tank for channel contents. */
public class ChannelTank extends FluidTankBase<ChannelBlockEntity> {
  private static final String TAG_LOCKED = "locked";
  private int locked;
  private final SnapshotJournal<Integer> lockedJournal = new SnapshotJournal<>() {
    @Override
    protected Integer createSnapshot() {
      return locked;
    }

    @Override
    protected void revertToSnapshot(Integer snapshot) {
      locked = snapshot;
    }
  };

  public ChannelTank(int capacity, ChannelBlockEntity parent) {
    super(capacity, parent);
  }

  /** Clears the per-tick extraction lock. */
  public void freeFluid() {
    locked = 0;
  }

  /** Returns the amount that was not inserted during the current tick. */
  public int getMaxUsable() {
    return Math.max(getFluidAmount() - locked, 0);
  }

  @Override
  public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
    int inserted = super.insert(index, resource, amount, transaction);
    if (inserted > 0) {
      lockedJournal.updateSnapshots(transaction);
      locked += inserted;
    }
    return inserted;
  }

  @Override
  protected void onContentsChanged(int index, FluidStack previousContents) {
    parent.setChanged();
    if (previousContents.isEmpty() != getFluid().isEmpty()) {
      parent.sendFluidUpdate();
    }
  }

  @Override
  public ChannelTank readFromNBT(HolderLookup.Provider provider, CompoundTag nbt) {
    locked = nbt.getIntOr(TAG_LOCKED, 0);
    deserialize(TagValueInput.create(ProblemReporter.DISCARDING, provider, nbt));
    return this;
  }

  @Override
  public CompoundTag writeToNBT(HolderLookup.Provider provider, CompoundTag nbt) {
    TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
    serialize(output);
    nbt.merge(output.buildResult());
    nbt.putInt(TAG_LOCKED, locked);
    return nbt;
  }
}
