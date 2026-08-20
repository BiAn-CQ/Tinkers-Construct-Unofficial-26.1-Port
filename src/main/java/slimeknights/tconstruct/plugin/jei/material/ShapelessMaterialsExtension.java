package slimeknights.tconstruct.plugin.jei.material;

import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.tconstruct.library.recipe.material.ShapelessMaterialsRecipe;

import java.util.List;

/** JEI 29 display extension for shapeless material-aware recipes. */
public final class ShapelessMaterialsExtension extends MaterialsCraftingExtension<ShapelessMaterialsRecipe> {
  public static final ShapelessMaterialsExtension INSTANCE = new ShapelessMaterialsExtension();

  private ShapelessMaterialsExtension() {}

  @Override
  protected List<Ingredient> getInputIngredients(ShapelessMaterialsRecipe recipe) {
    return recipe.getIngredients();
  }
}
