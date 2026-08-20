package slimeknights.tconstruct.library.recipe;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;

import java.util.List;

/**
 * Compatibility factory for the fluid ingredient helpers used by Tinkers recipes.
 *
 * <p>The 26.1 Mantle API exposes these factories directly on {@link FluidIngredient};
 * keeping this small forwarding class lets the migrated recipe code retain one
 * consistent call site while older integrations are compiled alongside it.</p>
 */
public final class FluidTinkerIngredients {
  public static final FluidIngredient EMPTY = FluidIngredient.EMPTY;

  private FluidTinkerIngredients() {}

  public static FluidIngredient of(Fluid fluid, int amount) {
    return FluidIngredient.of(fluid, amount);
  }

  public static FluidIngredient of(FluidStack stack) {
    return FluidIngredient.of(stack);
  }

  public static FluidIngredient of(TagKey<Fluid> tag, int amount) {
    return FluidIngredient.of(tag, amount);
  }

  public static FluidIngredient of(FluidIngredient... ingredients) {
    return FluidIngredient.of(ingredients);
  }

  public static FluidIngredient of(List<FluidIngredient> ingredients) {
    return FluidIngredient.of(ingredients);
  }
}
