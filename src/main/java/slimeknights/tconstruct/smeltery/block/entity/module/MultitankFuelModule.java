package slimeknights.tconstruct.smeltery.block.entity.module;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.EmptyResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.library.utils.TagUtil;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Supplier;

/** Fuel module that supports multiple tanks, selecting just one for the fuel result */
public class MultitankFuelModule extends FuelModule implements ResourceHandler<FluidResource> {
  /** Block position that will never be valid in world, used for sync */
  private static final BlockPos NULL_POS = new BlockPos(0, Short.MIN_VALUE, 0);

  /** Supplier for the list of valid tank positions */
  private final Supplier<List<BlockPos>> tankSupplier;
  /** Position of the last fluid handler */
  private BlockPos lastPos = NULL_POS;

  /** Map of all tank handlers at each relevant position. Used for fast switching between handlers, notably in the UI */
  private Map<BlockPos,TankCache> tankHandlers;

  private final class TankCache {
    private final BlockPos pos;
    @Nullable
    private final BlockCapabilityCache<ResourceHandler<FluidResource>,net.minecraft.core.Direction> cache;

    private TankCache(Level level, BlockPos pos) {
      this.pos = pos.immutable();
      this.cache = level instanceof ServerLevel serverLevel ? BlockCapabilityCache.create(
        Capabilities.Fluid.BLOCK, serverLevel, this.pos, null,
        () -> !parent.isRemoved() && tankHandlers != null && tankHandlers.get(this.pos) == this,
        () -> { if (this.pos.equals(lastPos)) fluidHandler = null; }) : null;
    }

    @Nullable
    private ResourceHandler<FluidResource> get() {
      return cache == null
        ? getLevel().getCapability(Capabilities.Fluid.BLOCK, pos, null)
        : cache.getCapability();
    }
  }

  public MultitankFuelModule(MantleBlockEntity parent, Supplier<List<BlockPos>> tankSupplier) {
    super(parent);
    this.tankSupplier = tankSupplier;
  }

  /** Resets just the last fluid listener */
  private void clearLastListener() {
    super.resetHandler();
  }

  @Override
  protected void resetHandler() {
    this.lastPos = NULL_POS;
    super.resetHandler();
  }

  /** Called on structure rebuild to clear the gui handler list */
  public void clearFluidListeners() {
    if (tankHandlers != null) {
      tankHandlers = null;
      fluidHandler = null;
    }
  }

  /** Called on servant load to ensure the listener is present in the cache */
  public void ensureTankPresent(BlockEntity be) {
    BlockPos pos = be.getBlockPos();
    if (tankHandlers != null && !tankHandlers.containsKey(pos)) {
      Level level = getLevel();
      TankCache cache = new TankCache(level, pos);
      if (cache.get() != null) {
        tankHandlers.put(pos.immutable(), cache);
      }
    }
  }

  /** Gets the map from position to fluid handler */
  private Map<BlockPos,TankCache> getTankHandlers() {
    if (tankHandlers == null) {
      tankHandlers = new LinkedHashMap<>();
      Level world = getLevel();
      for (BlockPos pos : tankSupplier.get()) {
        TankCache cache = new TankCache(world, pos);
        if (cache.get() != null) {
          tankHandlers.put(pos.immutable(), cache);
        }
      }
    }
    return tankHandlers;
  }

  @Nullable
  private ResourceHandler<FluidResource> getHandler(BlockPos pos) {
    Level level = getLevel();
    if (level instanceof ServerLevel) {
      TankCache cache = getTankHandlers().get(pos);
      return cache == null ? null : cache.get();
    }
    return level.getCapability(Capabilities.Fluid.BLOCK, pos, null);
  }


  /* Fuel finding */

  /**
   * Tries to consume fuel from the given position
   * @param pos  Position
   * @return   Temperature of the consumed fuel, 0 if none found
   */
  private int tryFuelPosition(BlockPos pos, boolean consume) {
    ResourceHandler<FluidResource> tankCap = getHandler(pos);
    if (tankCap != null) {
      // if we find a valid cap, try to consume fuel from it
      int temperature = tryLiquidFuel(tankCap, consume);
      if (temperature > 0) {
        clearLastListener();
        fluidHandler = tankCap;
        lastPos = pos;
        return temperature;
      }
    }
    return 0;
  }

  /**
   * Attempts to consume fuel from one of the tanks
   * @return  temperature of the found fluid, 0 if none
   */
  @Override
  public int findFuel(boolean consume) {
    // only fetch a handler if we haven't done so
    if (fluidHandler != null) {
      // if we have a handler, try to use that if possible
      int temperature = tryLiquidFuel(fluidHandler, consume);
      if (temperature > 0) {
        return temperature;
      }
    } else if (lastPos != NULL_POS) {
      // if no handler, try to find one at the last position
      int posTemp = tryFuelPosition(lastPos, consume);
      if (posTemp > 0) {
        return posTemp;
      }
    }

    // find a new handler among our tanks
    for (BlockPos pos : tankSupplier.get()) {
      // already checked the last position above, no reason to try again
      if (!pos.equals(lastPos)) {
        int posTemp = tryFuelPosition(pos, consume);
        if (posTemp > 0) {
          return posTemp;
        }
      }
    }

    // no handler found, tell client of the lack of fuel
    if (consume) {
      temperature = 0;
      rate = 0;
    }
    return 0;
  }


  /* NBT */
  private static final String TAG_LAST_FUEL = "last_fuel";

  @Override
  public void readFromTag(CompoundTag nbt) {
    super.readFromTag(nbt);
    if (nbt.contains(TAG_LAST_FUEL)) {
      lastPos = TagUtil.readBlockPos(nbt.get(TAG_LAST_FUEL)).offset(parent.getBlockPos());
    }
  }

  @Override
  public CompoundTag writeToTag(CompoundTag nbt) {
    nbt = super.writeToTag(nbt);
    if (lastPos != NULL_POS) {
      nbt.put(TAG_LAST_FUEL, TagUtil.writeBlockPos(lastPos.subtract(parent.getBlockPos())));
    }
    return nbt;
  }


  /* UI syncing */
  private static final int LAST_X = 4;
  private static final int LAST_Y = 5;
  private static final int LAST_Z = 6;

  @Override
  public int getCount() {
    return 7;
  }

  @Override
  public int get(int index) {
    return switch (index) {
      case LAST_X -> lastPos.getX();
      case LAST_Y -> lastPos.getY();
      case LAST_Z -> lastPos.getZ();
      default -> super.get(index);
    };
  }

  @Override
  public void set(int index, int value) {
    if (LAST_X <= index && index <= LAST_Z) {
      switch (index) {
        case LAST_X -> lastPos = new BlockPos(value, lastPos.getY(), lastPos.getZ());
        case LAST_Y -> lastPos = new BlockPos(lastPos.getX(), value, lastPos.getZ());
        case LAST_Z -> lastPos = new BlockPos(lastPos.getX(), lastPos.getY(), value);
      }
      clearLastListener();
    } else {
      super.set(index, value);
    }
  }

  @Override
  public FuelInfo getFuelInfo() {
    // if there is no position, means we have not yet consumed fuel. Just fetch the first tank
    // TODO: should we try to find a valid fuel tank? might be a bit confusing if they have multiple tanks in the structure before melting
    // however, a valid tank is a lot more effort to find

    // Y of big negative is how the UI syncs null
    BlockPos mainTank = lastPos;
    if (mainTank.getY() == NULL_POS.getY()) {
      // if no first, return no fuel info
      List<BlockPos> positions = tankSupplier.get();
      if (positions.isEmpty()) {
        return FuelInfo.EMPTY;
      }
      mainTank = positions.get(0);
      assert mainTank != null;
    }

    // fetch primary fuel handler
    if (fluidHandler == null) {
      ResourceHandler<FluidResource> fluidCap = getHandler(mainTank);
      if (fluidCap != null) {
        fluidHandler = fluidCap;
      }
    }

    // determine what fluid we have and hpw many other fluids we have
    FuelInfo info = super.getFuelInfo();
    // add extra fluid display
    if (!info.isEmpty()) {
      // add display info from each handler
      FluidStack currentFuel = info.getFluid();
      for (Entry<BlockPos,TankCache> entry : getTankHandlers().entrySet()) {
        if (!mainTank.equals(entry.getKey())) {
          ResourceHandler<FluidResource> handler = entry.getValue().get();
          if (handler != null && handler.size() > 0) {
            // sum if empty (more capacity) or the same fluid (more amount and capacity)
            FluidStack fluid = FluidUtil.getStack(handler, 0);
            int capacity = handler.getCapacityAsInt(0, handler.getResource(0));
            if (fluid.isEmpty()) {
              info.add(0, capacity);
            } else if (FluidStack.isSameFluidSameComponents(currentFuel, fluid)) {
              info.add(fluid.getAmount(), capacity);
            }
          }
        }
      }
    }

    return info;
  }


  /* Fluid handler */

  /** Gets the most recently used fluid */
  public FluidStack getLastFluid() {
    if (fluidHandler != null) {
      return fluidHandler.size() == 0 ? FluidStack.EMPTY : FluidUtil.getStack(fluidHandler, 0);
    }
    BlockPos pos;
    if (lastPos.getY() != NULL_POS.getY()) {
      pos = lastPos;
    } else {
      List<BlockPos> positions = tankSupplier.get();
      if (!positions.isEmpty()) {
        pos = positions.get(0);
      } else {
        return FluidStack.EMPTY;
      }
    }
    ResourceHandler<FluidResource> handler = getHandler(pos);
    return handler == null || handler.size() == 0 ? FluidStack.EMPTY : FluidUtil.getStack(handler, 0);
  }

  @Override
  public int size() {
    return tankSupplier.get().size();
  }

  /** Gets the native tank at the given logical index. */
  private ResourceHandler<FluidResource> getTank(int tank) {
    List<BlockPos> positions = tankSupplier.get();
    Objects.checkIndex(tank, positions.size());
    ResourceHandler<FluidResource> handler = getHandler(positions.get(tank));
    return handler == null ? EmptyResourceHandler.instance() : handler;
  }

  @Override
  public FluidResource getResource(int tank) {
    ResourceHandler<FluidResource> handler = getTank(tank);
    return handler.size() == 0 ? FluidResource.EMPTY : handler.getResource(0);
  }

  @Override
  public long getAmountAsLong(int tank) {
    ResourceHandler<FluidResource> handler = getTank(tank);
    return handler.size() == 0 ? 0 : handler.getAmountAsLong(0);
  }

  @Override
  public long getCapacityAsLong(int tank, FluidResource resource) {
    ResourceHandler<FluidResource> handler = getTank(tank);
    return handler.size() == 0 ? 0 : handler.getCapacityAsLong(0, resource);
  }

  @Override
  public boolean isValid(int tank, FluidResource resource) {
    ResourceHandler<FluidResource> handler = getTank(tank);
    return handler.size() > 0 && handler.isValid(0, resource);
  }

  @Override
  public int insert(int tank, FluidResource resource, int amount, TransactionContext transaction) {
    return getTank(tank).insert(resource, amount, transaction);
  }

  @Override
  public int insert(FluidResource resource, int amount, TransactionContext transaction) {
    int inserted = 0;
    for (TankCache cache : getTankHandlers().values()) {
      ResourceHandler<FluidResource> handler = cache.get();
      if (handler != null) {
        inserted += handler.insert(resource, amount - inserted, transaction);
        if (inserted == amount) {
          break;
        }
      }
    }
    return inserted;
  }

  @Override
  public int extract(int tank, FluidResource resource, int amount, TransactionContext transaction) {
    return getTank(tank).extract(resource, amount, transaction);
  }

  @Override
  public int extract(FluidResource resource, int amount, TransactionContext transaction) {
    int extracted = 0;
    for (TankCache cache : getTankHandlers().values()) {
      ResourceHandler<FluidResource> handler = cache.get();
      if (handler != null) {
        extracted += handler.extract(resource, amount - extracted, transaction);
        if (extracted == amount) {
          break;
        }
      }
    }
    return extracted;
  }
}
