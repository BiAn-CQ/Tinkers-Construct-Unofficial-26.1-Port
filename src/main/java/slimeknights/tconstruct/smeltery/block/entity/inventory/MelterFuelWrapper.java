package slimeknights.tconstruct.smeltery.block.entity.inventory;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import slimeknights.tconstruct.library.recipe.fuel.IFluidContainer;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;

import java.lang.ref.WeakReference;

/**
 * Fluid tank wrapper that weakly references a tank from a neighbor
 */
public class MelterFuelWrapper implements IFluidContainer {
  private final WeakReference<ResourceHandler<FluidResource>> tank;

  public MelterFuelWrapper(ResourceHandler<FluidResource> tank) {
    this.tank = new WeakReference<>(tank);
  }

  /**
   * Checks if this reference is still valid
   * @return  False if the stored tank is removed
   */
  public boolean isValid() {
    return this.tank.get() != null;
  }

  @Override
  public Fluid getFluid() {
    FluidStack fluid = getFluidStack();
    return fluid.isEmpty() ? Fluids.EMPTY : fluid.getFluid();
  }

  /* Melter methods */

  /**
   * Gets the contained fluid stack
   * @return  Contained fluid stack
   */
  public FluidStack getFluidStack() {
    ResourceHandler<FluidResource> handler = tank.get();
    return handler == null || handler.size() == 0 ? FluidStack.EMPTY : FluidUtil.getStack(handler, 0);
  }

  /**
   * Gets the capacity of the contained tank
   * @return  Tank capacity
   */
  public int getCapacity() {
    ResourceHandler<FluidResource> handler = tank.get();
    return handler == null || handler.size() == 0 ? 0 : handler.getCapacityAsInt(0, handler.getResource(0));
  }

  /**
   * Drains one copy of fuel from the given tank
   * @param fuel  Fuel to drain
   * @return  Ticks of fuel units
   */
  public int consumeFuel(MeltingFuel fuel) {
    ResourceHandler<FluidResource> tank = this.tank.get();
    if (tank != null && tank.size() > 0) {
      int amount = fuel.getAmount(this);
      if (amount > 0) {
        FluidResource resource = tank.getResource(0);
        try (Transaction transaction = Transaction.open(null)) {
          int drained = tank.extract(0, resource, amount, transaction);
          if (drained > 0) {
            transaction.commit();
          }
          int duration = fuel.getDuration();
          return drained < amount ? duration * drained / amount : duration;
        }
      }
    }
    return 0;
  }
}
