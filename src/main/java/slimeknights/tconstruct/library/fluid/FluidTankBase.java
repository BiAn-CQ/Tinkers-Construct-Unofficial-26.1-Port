package slimeknights.tconstruct.library.fluid;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.smeltery.network.FluidUpdatePacket;

import java.util.function.Predicate;

/** Single fluid tank backed by NeoForge's transactional transfer API. */
public class FluidTankBase<T extends MantleBlockEntity> extends FluidStacksResourceHandler {
  protected final T parent;
  private Predicate<FluidStack> validator = stack -> true;

  public FluidTankBase(int capacity, T parent) {
    super(1, capacity);
    this.parent = parent;
  }

  /** Reads this tank from the block entity's nested tank tag. */
  public FluidTankBase<T> readFromNBT(HolderLookup.Provider provider, CompoundTag nbt) {
    deserialize(TagValueInput.create(ProblemReporter.DISCARDING, provider, nbt));
    return this;
  }

  /** Writes this tank to the block entity's nested tank tag. */
  public CompoundTag writeToNBT(HolderLookup.Provider provider, CompoundTag nbt) {
    TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
    serialize(output);
    nbt.merge(output.buildResult());
    return nbt;
  }

  public FluidTankBase<T> setCapacity(int capacity) {
    this.capacity = capacity;
    return this;
  }

  public FluidTankBase<T> setValidator(Predicate<FluidStack> validator) {
    if (validator != null) {
      this.validator = validator;
    }
    return this;
  }

  @Override
  public boolean isValid(int index, FluidResource resource) {
    return index == 0 && !resource.isEmpty() && validator.test(resource.toStack(1));
  }

  @Override
  protected void onContentsChanged(int index, FluidStack previousContents) {
    onContentsChanged();
  }

  /** Notifies the parent after a direct or committed transactional change. */
  public void onContentsChanged() {
    if (parent instanceof IFluidTankUpdater updater) {
      updater.onTankContentsChanged();
    }

    parent.setChanged();
    Level level = parent.getLevel();
    if (level != null && !level.isClientSide()) {
      TinkerNetwork.getInstance().sendToClientsAround(new FluidUpdatePacket(parent.getBlockPos(), getFluid()), level, parent.getBlockPos());
    }
  }

  public FluidStack getFluid() {
    return stacks.getFirst();
  }

  public int getFluidAmount() {
    return getAmountAsInt(0);
  }

  public int getCapacity() {
    return capacity;
  }

  public void setFluid(FluidStack stack) {
    stacks.set(0, stack);
  }

  public boolean isEmpty() {
    return getFluid().isEmpty();
  }

  public int getSpace() {
    return Math.max(0, capacity - getFluidAmount());
  }

  public boolean isFluidValid(FluidStack stack) {
    return !stack.isEmpty() && validator.test(stack);
  }

}
