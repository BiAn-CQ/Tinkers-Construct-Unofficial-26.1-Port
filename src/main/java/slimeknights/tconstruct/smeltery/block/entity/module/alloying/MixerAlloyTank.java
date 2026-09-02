package slimeknights.tconstruct.smeltery.block.entity.module.alloying;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.EmptyResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.recipe.alloying.IMutableAlloyTank;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * Alloy tank that takes inputs from neighboring blocks
 */
@RequiredArgsConstructor
public class MixerAlloyTank implements IMutableAlloyTank {
  // parameters
  /** Handler parent */
  private final MantleBlockEntity parent;
  /** Tank for outputs */
  private final ResourceHandler<FluidResource> outputTank;

  /** Current temperature. Provided as a getter and setter as there are a few contexts with different source for temperature */
  @Getter
  @Setter
  private int temperature = 0;

  // side tank cache
  /** Cache of tanks for each of the sides */
  private final Map<Direction,InputCache> inputs = new EnumMap<>(Direction.class);
  /** Map of tank index to tank on the side */
  @Nullable
  private ResourceHandler<FluidResource>[] indexedList = null;

  // state
  /** If true, tanks are marked for refresh later */
  private boolean needsRefresh = true;
  /** Number of currently held tanks */
  private int currentTanks = 0;

  private final class InputCache {
    private final Direction direction;
    private final BlockPos target;
    @Nullable
    private final BlockCapabilityCache<ResourceHandler<FluidResource>,Direction> cache;
    private boolean present;

    private InputCache(Level level, Direction direction) {
      this.direction = direction;
      this.target = parent.getBlockPos().relative(direction);
      this.cache = level instanceof ServerLevel serverLevel ? BlockCapabilityCache.create(
        Capabilities.Fluid.BLOCK, serverLevel, target, direction.getOpposite(),
        () -> !parent.isRemoved() && inputs.get(direction) == this,
        () -> refresh(direction, true)) : null;
    }

    @Nullable
    private ResourceHandler<FluidResource> get() {
      Level level = parent.getLevel();
      ResourceHandler<FluidResource> handler = level == null ? null : cache == null
        ? level.getCapability(Capabilities.Fluid.BLOCK, target, direction.getOpposite())
        : cache.getCapability();
      present = handler != null;
      return handler;
    }
  }

  @Override
  public int getTanks() {
    checkTanks();
    return currentTanks;
  }

  /** Gets the map of index to direction */
  @SuppressWarnings("unchecked")
  private ResourceHandler<FluidResource>[] indexTanks() {
    // convert map into indexed list of fluid handlers, will be cleared next time a side updates
    if (indexedList == null) {
      indexedList = (ResourceHandler<FluidResource>[])new ResourceHandler<?>[currentTanks];
      if (currentTanks > 0) {
        int nextTank = 0;
        for (Direction direction : Direction.values()) {
          if (direction != Direction.DOWN) {
            InputCache cache = inputs.get(direction);
            ResourceHandler<FluidResource> handler = cache == null ? null : cache.get();
            if (handler != null) {
              indexedList[nextTank] = handler;
              nextTank++;
            }
          }
        }
      }
    }
    return indexedList;
  }

  /** Gets the fluid handler for the given tank index */
  public ResourceHandler<FluidResource> getFluidHandler(int tank) {
    checkTanks();
    // invalid index, nothing
    if (tank >= currentTanks || tank < 0) {
      return EmptyResourceHandler.instance();
    }
    return indexTanks()[tank];
  }

  @Override
  public FluidStack getFluidInTank(int tank) {
    checkTanks();
    // invalid index, nothing
    if (tank >= currentTanks || tank < 0) {
      return FluidStack.EMPTY;
    }
    // get the first fluid from the proper tank, we do not support multiple fluids on a side
    ResourceHandler<FluidResource> handler = indexTanks()[tank];
    return handler.size() == 0 ? FluidStack.EMPTY : FluidUtil.getStack(handler, 0);
  }

  @Override
  public FluidStack drain(int tank, FluidStack fluidStack) {
    checkTanks();
    // invalid index, nothing
    if (tank >= currentTanks || tank < 0) {
      return FluidStack.EMPTY;
    }
    if (fluidStack.isEmpty()) {
      return FluidStack.EMPTY;
    }
    FluidResource resource = FluidResource.of(fluidStack);
    try (Transaction transaction = Transaction.openRoot()) {
      int extracted = indexTanks()[tank].extract(resource, fluidStack.getAmount(), transaction);
      transaction.commit();
      return resource.toStack(extracted);
    }
  }

  @Override
  public boolean canFit(FluidStack fluid, int removed) {
    checkTanks();
    if (fluid.isEmpty()) {
      return true;
    }
    try (Transaction transaction = Transaction.openRoot()) {
      return outputTank.insert(FluidResource.of(fluid), fluid.getAmount(), transaction) == fluid.getAmount();
    }
  }

  @Override
  public int fill(FluidStack fluidStack) {
    if (fluidStack.isEmpty()) {
      return 0;
    }
    try (Transaction transaction = Transaction.openRoot()) {
      int inserted = outputTank.insert(FluidResource.of(fluidStack), fluidStack.getAmount(), transaction);
      if (inserted > 0) {
        transaction.commit();
      }
      return inserted;
    }
  }

  /**
   * Refreshes the cached tanks if needed
   * After calling this method, all five tank sides will have been fetched
   */
  private void checkTanks() {
    // need world to do anything
    Level world = parent.getLevel();
    if (world == null) {
      return;
    }
    if (needsRefresh) {
      for (Direction direction : Direction.values()) {
        // update each direction we are missing
        if (direction != Direction.DOWN && !inputs.containsKey(direction)) {
          BlockPos target = parent.getBlockPos().relative(direction);
          // limit by blocks as that gives the modpack more control, say they want to allow only scorched tanks
          if (world.getBlockState(target).is(TinkerTags.Blocks.ALLOYER_TANKS)) {
            InputCache cache = new InputCache(world, direction);
            inputs.put(direction, cache);
            if (cache.get() != null) {
              currentTanks++;
            }
          }
        }
      }
      needsRefresh = false;
    }
  }

  /**
   * Called on block update or when a capability invalidates to mark that a direction needs updates
   * @param direction  Side updating
   * @param checkInput If true, validates that the side contains an input before reducing tank count. False when invalidated through the capability
   * */
  public void refresh(Direction direction, boolean checkInput) {
    if (direction == Direction.DOWN) {
      return;
    }
    InputCache cache = inputs.get(direction);
    if (!checkInput || (cache != null && cache.present)) {
      currentTanks--;
    }
    inputs.remove(direction);
    needsRefresh = true;
    indexedList = null;
  }
}
