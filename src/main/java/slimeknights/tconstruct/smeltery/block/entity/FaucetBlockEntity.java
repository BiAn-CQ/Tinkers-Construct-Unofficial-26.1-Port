package slimeknights.tconstruct.smeltery.block.entity;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import javax.annotation.Nullable;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.utils.FluidStackDataUtil;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.component.SmelteryInputOutputBlockEntity.SmelteryFluidIO;
import slimeknights.tconstruct.smeltery.block.entity.tank.ISmelteryTankHandler;
import slimeknights.tconstruct.smeltery.network.FaucetActivationPacket;

import static slimeknights.tconstruct.smeltery.block.FaucetBlock.FACING;

public class FaucetBlockEntity extends MantleBlockEntity {
  /** amount of MB to extract from the input at a time */
  public static final int PACKET_SIZE = FluidValues.INGOT;
  /** Transfer rate of the faucet */
  public static final int MB_PER_TICK = FluidValues.NUGGET;

  public static final BlockEntityTicker<FaucetBlockEntity> SERVER_TICKER = (level, pos, world, self) -> self.tick();

  private static final String TAG_DRAINED = "drained";
  private static final String TAG_RENDER_FLUID = "render_fluid";
  private static final String TAG_STOP = "stop";
  private static final String TAG_STATE = "state";
  private static final String TAG_LAST_REDSTONE = "lastRedstone";

  /** If true, faucet is currently pouring */
  private FaucetState faucetState = FaucetState.OFF;
  /** If true, redstone told this faucet to stop, so stop when ready */
  private boolean stopPouring = false;
  /** Current fluid in the faucet */
  private FluidStack drained = FluidStack.EMPTY;
  /** Fluid for rendering, used to reduce the number of packets. There is a brief moment where {@link this#drained} is empty but we should be rendering something */
  @Getter
  private FluidStack renderFluid = FluidStack.EMPTY;
  /** Used for pulse detection */
  private boolean lastRedstoneState = false;

  /** Fluid handler of the input to the faucet */
  private FluidCache inputHandler;
  /** Fluid handler of the output from the faucet */
  private FluidCache outputHandler;

  public FaucetBlockEntity(BlockPos pos, BlockState state) {
    this(TinkerSmeltery.faucet.get(), pos, state);
  }

  @SuppressWarnings("WeakerAccess")
  protected FaucetBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }


  /* Fluid handler */

  /**
   * Finds the fluid handler on the given side
   * @param side  Side to check
   * @return  Fluid handler
   */
  private final class FluidCache {
    private final boolean input;
    private final BlockPos target;
    private final Direction context;
    private final BlockCapabilityCache<ResourceHandler<FluidResource>,Direction> cache;

    private FluidCache(ServerLevel serverLevel, Direction side, boolean input) {
      this.input = input;
      this.target = worldPosition.relative(side);
      this.context = side.getOpposite();
      cache = BlockCapabilityCache.create(
        Capabilities.Fluid.BLOCK, serverLevel, target, context,
        () -> !isRemoved() && (input ? inputHandler : outputHandler) == this,
        () -> {
          if (input) inputHandler = null;
          else outputHandler = null;
        });
    }

    @Nullable
    private IFluidHandler get() {
      IFluidHandler handler = slimeknights.tconstruct.library.utils.TinkerCapabilityAdapters.fluidHandler(cache.getCapability());
      return handler != null ? handler : findDirectFluidHandler(target, context);
    }
  }

  /**
   * Compatibility fallback for Tinkers block entities whose handlers are intentionally internal or
   * whose NeoForge capability has not been rebuilt yet. This follows the original direct block-entity
   * path instead of silently making the faucet inert.
   */
  @Nullable
  private IFluidHandler findDirectFluidHandler(BlockPos target, @Nullable Direction context) {
    if (level == null) {
      return null;
    }
    BlockEntity blockEntity = level.getBlockEntity(target);
    if (blockEntity instanceof SmelteryFluidIO fluidIO) {
      return fluidIO.getProxiedCapability(context);
    }
    if (blockEntity instanceof ISmelteryTankHandler tankHandler) {
      return tankHandler.getFluidCapability();
    }
    if (blockEntity instanceof ITankBlockEntity tankBlockEntity) {
      return tankBlockEntity.getTank();
    }
    if (blockEntity instanceof CastingBlockEntity castingBlockEntity) {
      return castingBlockEntity.getTank();
    }
    return null;
  }

  @Nullable
  private IFluidHandler findFluidHandler(Direction side, boolean input) {
    assert level != null;
    if (level instanceof ServerLevel serverLevel) {
      FluidCache cache = new FluidCache(serverLevel, side, input);
      if (input) inputHandler = cache;
      else outputHandler = cache;
      return cache.get();
    }
    BlockPos target = worldPosition.relative(side);
    Direction context = side.getOpposite();
    IFluidHandler handler = slimeknights.tconstruct.library.utils.TinkerCapabilityAdapters.fluidHandler(level.getCapability(Capabilities.Fluid.BLOCK, target, context));
    return handler != null ? handler : findDirectFluidHandler(target, context);
  }

  /**
   * Gets the input fluid handler
   * @return  Input fluid handler
   */
  @Nullable
  private IFluidHandler getInputHandler() {
    if (inputHandler == null) {
      return findFluidHandler(getBlockState().getValue(FACING).getOpposite(), true);
    }
    return inputHandler.get();
  }

  /**
   * Gets the output fluid handler
   * @return  Output fluid handler
   */
  @Nullable
  private IFluidHandler getOutputHandler() {
    if (outputHandler == null) {
      return findFluidHandler(Direction.DOWN, false);
    }
    return outputHandler.get();
  }

  /**
   * Called when a neighbor changes to invalidate the cached fluid handler
   * @param neighbor  Neighbor position that changed
   */
  public void neighborChanged(BlockPos neighbor) {
    // if the neighbor was below us, remove output
    if (worldPosition.equals(neighbor.above())) {
      outputHandler = null;
      // neighbor behind us
    } else if (worldPosition.equals(neighbor.relative(getBlockState().getValue(FACING)))) {
      inputHandler = null;
    }
  }

  /** Invalidates both transfer endpoints when the neighbor update has no known source direction. */
  public void neighborsChanged() {
    inputHandler = null;
    outputHandler = null;
  }


  /* Data */

  /**
   * Gets whether the faucet is pouring
   * @return True if pouring
   */
  public boolean isPouring() {
    return faucetState != FaucetState.OFF;
  }

  /* Activation */

  /**
   * Toggles pouring state and initiates transfer if appropriate. Called on right click and from redstone
   */
  public void activate() {
    // don't run on client
    if (level == null || level.isClientSide()) {
      return;
    }
    // already pouring? we want to start
    switch (faucetState) {
      // off activates the faucet
      case OFF -> {
        stopPouring = false;
        doTransfer(true);
      }
      // powered deactivates the faucet, sync to client
      case POWERED -> {
        faucetState = FaucetState.OFF;
        syncToClient(FluidStack.EMPTY, false);
      }
      // pouring means we stop pouring as soon as possible
      case POURING -> stopPouring = true;
    }
  }

  /**
   * Flips hasSignal and schedules a tick if appropriate.
   * @param hasSignal  New signal state
   */
  public void handleRedstone(boolean hasSignal) {
    if (hasSignal != lastRedstoneState) {
      lastRedstoneState = hasSignal;
      if (hasSignal) {
        if (level != null){
          level.scheduleTick(worldPosition, this.getBlockState().getBlock(), 2);
        }
      } else if (faucetState == FaucetState.POWERED) {
        faucetState = FaucetState.OFF;
        syncToClient(FluidStack.EMPTY, false);
      }
    }
  }


  /* Pouring */

  /** Handles server ticks */
  private void tick() {
    // nothing to do if not pouring
    if (faucetState == FaucetState.OFF) {
      return;
      // if powered and we can transfer, schedule transfer for next tick
    } else if (faucetState == FaucetState.POWERED && doTransfer(false)) {
      faucetState = FaucetState.POURING;
      return;
    }

    // continue current stack
    if (!drained.isEmpty()) {
      pour();
      // stop if told to stop once done
    } else if (stopPouring) {
      reset();
      // otherwise keep going
    } else {
      doTransfer(true);
    }
  }

  /**
   * Initiate fluid transfer
   */
  private boolean doTransfer(boolean execute) {
    // still got content left
    IFluidHandler input = getInputHandler();
    IFluidHandler output = getOutputHandler();
    if (execute && (input == null || output == null)) {
      TConstruct.LOG.debug("Faucet at {} cannot start: input handler={}, output handler={}", worldPosition, input, output);
    }
    if (input != null && output != null) {
      // can we drain?
      FluidStack drained = input.drain(PACKET_SIZE, FluidAction.SIMULATE);
      if (execute && drained.isEmpty()) {
        TConstruct.LOG.debug("Faucet at {} found an input handler but no drainable fluid", worldPosition);
      }
      if (!drained.isEmpty()) {
        // can we fill
        int filled = output.fill(drained, FluidAction.SIMULATE);
        if (execute && filled <= 0) {
          TConstruct.LOG.debug("Faucet at {} output {} rejected {} mB of {}", worldPosition,
                               output.getClass().getName(), drained.getAmount(), drained.getFluid());
        }
        if (filled > 0) {
          // ensure we can actually fill in our min increment, deals with handlers like copper cans
          // can skip this step if we already received a small enough number
          drained.setAmount(MB_PER_TICK); // done using this fluid stack's original size, so save some memory and reuse
          if (filled <= MB_PER_TICK || output.fill(drained, FluidAction.SIMULATE) > 0) {
            // execute if requested
            if (execute) {
              // drain the liquid and transfer it, buffer the amount for delay
              this.drained = input.drain(filled, FluidAction.EXECUTE);

              // sync to clients if we have changes
              if (faucetState == FaucetState.OFF || !FluidStack.isSameFluidSameComponents(renderFluid, drained)) {
                syncToClient(this.drained, true);
              }
              faucetState = FaucetState.POURING;
              // pour after initial packet, in case we end up resetting later
              pour();
            }
            return true;
          }
        }
      }

      // if powered, keep faucet running
      if (lastRedstoneState) {
        // sync if either we were not pouring before (particle effects), or if the client thinks we have fluid
        if (execute && (faucetState == FaucetState.OFF || !FluidStack.isSameFluidSameComponents(renderFluid, FluidStack.EMPTY))) {
          syncToClient(FluidStack.EMPTY, true);
        }
        faucetState = FaucetState.POWERED;
        return false;
      }
    }
    // reset if not powered, or if nothing to do
    if (execute) {
      reset();
    }
    return false;
  }

  /**
   * Takes the liquid inside and executes one pouring step.
   */
  private void pour() {
    if (drained.isEmpty()) {
      return;
    }

    // ensure we have an output
    IFluidHandler output = getOutputHandler();
    if (output != null) {
      FluidStack fillStack = drained.copy();
      fillStack.setAmount(Math.min(drained.getAmount(), MB_PER_TICK));

      // can we fill?
      int filled = output.fill(fillStack, IFluidHandler.FluidAction.SIMULATE);
      if (filled > 0) {
        // update client if they do not think we have fluid
        if (!FluidStack.isSameFluidSameComponents(renderFluid, drained)) {
          syncToClient(drained, true);
        }

        // transfer it
        this.drained.shrink(filled);
        fillStack.setAmount(filled);
        output.fill(fillStack, IFluidHandler.FluidAction.EXECUTE);
      }
    }
    else {
      // output got lost. all liquid buffered is lost.
      reset();
    }
  }

  /**
   * Resets TE to default state.
   */
  private void reset() {
    stopPouring = false;
    drained = FluidStack.EMPTY;
    if (faucetState != FaucetState.OFF || !FluidStack.isSameFluidSameComponents(renderFluid, drained)) {
      faucetState = FaucetState.OFF;
      syncToClient(FluidStack.EMPTY, false);
    }
  }

  public AABB getRenderBoundingBox() {
    return new AABB(worldPosition.getX(), worldPosition.getY() - 1, worldPosition.getZ(), worldPosition.getX() + 1, worldPosition.getY() + 1, worldPosition.getZ() + 1);
  }


  /* NBT and networking */

  /**
   * Sends an update to the client with the most recent
   * @param fluid       New fluid
   * @param isPouring   New isPouring status
   */
  private void syncToClient(FluidStack fluid, boolean isPouring) {
    renderFluid = fluid.copy();
    if (level instanceof ServerLevel) {
      TinkerNetwork.getInstance().sendToClientsAround(new FaucetActivationPacket(worldPosition, fluid, isPouring), (ServerLevel) level, getBlockPos());
    }
  }

  /**
   * Sets draining fluid to specified stack.
   * @param fluid new FluidStack
   */
  public void onActivationPacket(FluidStack fluid, boolean isPouring) {
    // pouring and powered are interchangable on the client
    this.faucetState = isPouring ? FaucetState.POURING : FaucetState.OFF;
    this.renderFluid = fluid;
  }

  @Override
  protected boolean shouldSyncOnUpdate() {
    return true;
  }

  @Override
  protected void saveSynced(CompoundTag compound, HolderLookup.Provider provider) {
    super.saveSynced(compound, provider);
    compound.putByte(TAG_STATE, (byte)faucetState.ordinal());
    if (!renderFluid.isEmpty()) {
      compound.put(TAG_RENDER_FLUID, FluidStackDataUtil.save(provider, renderFluid));
    }
  }

  @Override
  public void saveAdditional(CompoundTag compound) {
    super.saveAdditional(compound);
    compound.putBoolean(TAG_STOP, stopPouring);
    compound.putBoolean(TAG_LAST_REDSTONE, lastRedstoneState);
    if (!drained.isEmpty()) {
      compound.put(TAG_DRAINED, FluidStackDataUtil.save(registries(), drained));
    }
  }

  @Override
  public void load(CompoundTag compound) {
    super.load(compound);

    faucetState = FaucetState.fromIndex(compound.getByteOr(TAG_STATE, (byte)0));
    stopPouring = compound.getBooleanOr(TAG_STOP, false);
    lastRedstoneState = compound.getBooleanOr(TAG_LAST_REDSTONE, false);
    // fluids
    if (compound.contains(TAG_DRAINED)) {
      drained = FluidStackDataUtil.parse(registries(), compound.getCompoundOrEmpty(TAG_DRAINED));
    } else {
      drained = FluidStack.EMPTY;
    }
    if (compound.contains(TAG_RENDER_FLUID)) {
      renderFluid = FluidStackDataUtil.parse(registries(), compound.getCompoundOrEmpty(TAG_RENDER_FLUID));
    } else {
      renderFluid = FluidStack.EMPTY;
    }
  }

  private enum FaucetState {
    OFF,
    POURING,
    POWERED;

    /** Gets the state for the given index */
    public static FaucetState fromIndex(int index) {
      switch (index) {
        case 1: return POURING;
        case 2: return POWERED;
      }
      return OFF;
    }
  }
}
