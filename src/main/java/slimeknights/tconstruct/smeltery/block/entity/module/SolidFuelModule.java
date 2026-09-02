package slimeknights.tconstruct.smeltery.block.entity.module;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.EmptyResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuelLookup;

import javax.annotation.Nullable;

/** Fuel module variant that supports both item and fluid fuels. Only supports a single fluid position which should not change. */
public class SolidFuelModule extends FuelModule {
  /** Location of the fuel tank */
  private final BlockPos fuelPos;
  /** Last item handler where items were extracted */
  @Nullable
  private ResourceHandler<ItemResource> itemHandler;
  @Nullable
  private BlockCapabilityCache<ResourceHandler<FluidResource>,net.minecraft.core.Direction> fluidCache;
  @Nullable
  private BlockCapabilityCache<ResourceHandler<ItemResource>,net.minecraft.core.Direction> itemCache;

  public SolidFuelModule(MantleBlockEntity parent, BlockPos fuelPos) {
    super(parent);
    this.fuelPos = fuelPos;
  }

  @Override
  protected void resetHandler() {
    super.resetHandler();
    itemHandler = null;
    fluidCache = null;
    itemCache = null;
  }


  /* Fuel updating */

  /**
   * Tries to consume fuel from the given fluid handler
   * @param handler  Handler to consume fuel from
   * @return   Temperature of the consumed fuel, 0 if none found
   */
  private int trySolidFuel(ResourceHandler<ItemResource> handler, boolean consume) {
    for (int i = 0; i < handler.size(); i++) {
      ItemStack stack = ItemUtil.getStack(handler, i);
      int time = stack.getBurnTime(TinkerRecipeTypes.FUEL.get(), getLevel().fuelValues()) / 4;
      if (time > 0) {
        MeltingFuel solid = MeltingFuelLookup.getSolid();
        if (consume) {
          ItemResource resource = handler.getResource(i);
          var remainder = stack.getCraftingRemainder();
          ItemStack container = remainder == null ? ItemStack.EMPTY : remainder.create();
          int leftoverContainer = 0;
          try (Transaction transaction = Transaction.openRoot()) {
            if (handler.extract(i, resource, 1, transaction) == 1) {
              if (!container.isEmpty()) {
                leftoverContainer = container.getCount() - handler.insert(ItemResource.of(container), container.getCount(), transaction);
              }
              transaction.commit();
            } else {
              TConstruct.LOG.error("Invalid item removed from solid fuel handler");
              return 0;
            }
          }
          fuel += time;
          fuelQuality = time;
          temperature = solid.getTemperature();
          rate = solid.getRate();
          parent.setChangedFast();
          if (leftoverContainer > 0) {
            Level world = getLevel();
            double x = (world.getRandom().nextFloat() * 0.5F) + 0.25D;
            double y = (world.getRandom().nextFloat() * 0.5F) + 0.25D;
            double z = (world.getRandom().nextFloat() * 0.5F) + 0.25D;
            ItemEntity itemEntity = new ItemEntity(world, fuelPos.getX() + x, fuelPos.getY() + y, fuelPos.getZ() + z, container.copyWithCount(leftoverContainer));
            itemEntity.setDefaultPickUpDelay();
            world.addFreshEntity(itemEntity);
          }
        }
        return solid.getTemperature();
      }
    }
    return 0;
  }

  /** Fetches any relevant fuel handlers from the target position */
  private void fetchHandlers() {
    Level level = getLevel();
    if (level instanceof ServerLevel serverLevel) {
      if (fluidCache == null) {
        fluidCache = BlockCapabilityCache.create(Capabilities.Fluid.BLOCK, serverLevel, fuelPos, null,
          () -> !parent.isRemoved(), () -> fluidHandler = null);
      }
      if (itemCache == null) {
        itemCache = BlockCapabilityCache.create(Capabilities.Item.BLOCK, serverLevel, fuelPos, null,
          () -> !parent.isRemoved(), () -> itemHandler = null);
      }
      fluidHandler = fluidCache.getCapability();
      itemHandler = itemCache.getCapability();
    } else {
      fluidHandler = level.getCapability(Capabilities.Fluid.BLOCK, fuelPos, null);
      itemHandler = level.getCapability(Capabilities.Item.BLOCK, fuelPos, null);
    }
  }

  @Override
  public int findFuel(boolean consume) {
    fetchHandlers();
    // prioritize liquid fuel - it usually goes hotter
    int temperature = 0;
    if (fluidHandler != null) {
      temperature = tryLiquidFuel(fluidHandler, consume);
    }
    // next, try solid fuel
    if (temperature == 0 && itemHandler != null) {
      temperature = trySolidFuel(itemHandler, consume);
    }
    // no handler found, tell client of the lack of fuel
    if (temperature == 0 && consume) {
      this.temperature = 0;
      this.rate = 0;
    }
    return temperature;
  }


  /* UI Syncing */

  @Override
  public FuelInfo getFuelInfo() {
    fetchHandlers();
    FuelInfo info = super.getFuelInfo();
    if (info.isEmpty() && itemHandler != null) {
      return FuelInfo.ITEM;
    }
    return info;
  }


  /* Fluid handler */

  /** Gets the fluid handler for proxy */
  public ResourceHandler<FluidResource> getTank() {
    return fluidHandler == null ? EmptyResourceHandler.instance() : fluidHandler;
  }
}
