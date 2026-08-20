package slimeknights.tconstruct.library.recipe.casting;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import slimeknights.mantle.recipe.container.ISingleStackContainer;

import javax.annotation.Nullable;

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

  /**
   * Gets the NBT for the contained fluid
   * @return  Fluid's NBT
   */
  @Nullable
  default CompoundTag getFluidTag() {
    CompoundTag tag = getFluidStack().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    return tag.isEmpty() ? null : tag;
  }
}
