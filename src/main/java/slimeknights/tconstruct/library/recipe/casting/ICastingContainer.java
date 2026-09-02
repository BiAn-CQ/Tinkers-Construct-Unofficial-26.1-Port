package slimeknights.tconstruct.library.recipe.casting;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.recipe.container.ISingleStackContainer;

/**
 * Inventory containing a single item and a fluid
 */
public interface ICastingContainer extends ISingleStackContainer {
  /**
   * Gets the contained fluid in this inventory
   * @return  Contained fluid
   */
  default Fluid getFluid() {
    return getFluidStack().getFluid();
  }

  /** Gets the contained fluid stack, including its data components. */
  FluidStack getFluidStack();
}
