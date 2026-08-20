package slimeknights.tconstruct.smeltery.block.entity.inventory;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.tconstruct.library.recipe.casting.ICastingContainer;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;

import javax.annotation.Nullable;

/**
 * Provides read only access to the input of a casting table. Prevents extra data from leaking
 */
@RequiredArgsConstructor
public class CastingContainerWrapper implements ICastingContainer {
  private final CastingBlockEntity tile;
  @Setter
  private FluidStack fluid;
  private boolean switchSlots = false;

  public CastingContainerWrapper(CastingBlockEntity tile, FluidStack fluid) {
    this.tile = tile;
    this.fluid = fluid;
  }

  @Override
  public ItemStack getStack() {
    ItemStack stack = tile.getItem(switchSlots ? CastingBlockEntity.OUTPUT : CastingBlockEntity.INPUT);
    if (stack.is(tile.getEmptyCastTag())) {
      return ItemStack.EMPTY;
    }
    return stack;
  }

  /**
   * Minecraft 1.21 skips recipe lookup when {@link net.minecraft.world.item.crafting.RecipeInput#isEmpty()}
   * returns true. Casting inputs may legitimately contain only fluid (basin recipes and cast-less table
   * recipes), so the inherited single-item implementation is not sufficient here.
   */
  @Override
  public boolean isEmpty() {
    return getStack().isEmpty() && (fluid == null || fluid.isEmpty());
  }

  @Override
  public FluidStack getFluidStack() {
    return fluid;
  }

  @Nullable
  @Override
  public CompoundTag getFluidTag() {
    return fluid.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
  }

  /** Uses the input for input (default) */
  public void useInput() {
    switchSlots = false;
  }

  /** Uses the output for input (for multistep casting) */
  public void useOutput() {
    switchSlots = true;
  }
}
