package slimeknights.tconstruct.plugin.jei.material;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import slimeknights.tconstruct.library.recipe.material.ShapedMaterialsRecipe;

import java.util.List;
import java.util.stream.IntStream;

/** JEI 29 display extension for shaped material-aware recipes. */
public final class ShapedMaterialsExtension extends MaterialsCraftingExtension<ShapedMaterialsRecipe> {
  public static final ShapedMaterialsExtension INSTANCE = new ShapedMaterialsExtension();

  private ShapedMaterialsExtension() {}

  @Override
  protected List<Ingredient> getInputIngredients(ShapedMaterialsRecipe recipe) {
    return recipe.getInputIngredients();
  }

  @Override
  protected int[] getMaterialSlots(ShapedMaterialsRecipe recipe, Ingredient firstPart) {
    List<Ingredient> inputs = recipe.getInputIngredients();
    return IntStream.range(0, inputs.size()).filter(i -> inputs.get(i) == firstPart).toArray();
  }

  @Override
  public int getWidth(RecipeHolder<ShapedMaterialsRecipe> holder) {
    return holder.value().getWidth();
  }

  @Override
  public int getHeight(RecipeHolder<ShapedMaterialsRecipe> holder) {
    return holder.value().getHeight();
  }
}
