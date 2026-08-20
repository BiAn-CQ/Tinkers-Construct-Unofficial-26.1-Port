package slimeknights.tconstruct.plugin.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import slimeknights.tconstruct.library.recipe.TinkerIngredients;
import slimeknights.tconstruct.library.recipe.ingredient.MaterialValueIngredient;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;
import slimeknights.tconstruct.library.recipe.material.ShapedMaterialRecipe;
import slimeknights.tconstruct.plugin.jei.material.MaterialsCraftingExtension;

import java.util.List;
import java.util.stream.IntStream;

/** JEI 29 display extension for the deprecated single-material shaped recipe. */
@SuppressWarnings("deprecation")
public final class ShapedMaterialExtension implements ICraftingCategoryExtension<ShapedMaterialRecipe> {
  public static final ShapedMaterialExtension INSTANCE = new ShapedMaterialExtension();

  private ShapedMaterialExtension() {}

  private static List<Ingredient> inputs(ShapedMaterialRecipe recipe) {
    return recipe.getIngredients().stream().map(optional -> optional.orElse(TinkerIngredients.EMPTY)).toList();
  }

  @Override
  public boolean isHandled(RecipeHolder<ShapedMaterialRecipe> holder) {
    return holder.value().getMaterial() != null;
  }

  @Override
  public List<SlotDisplay> getIngredients(RecipeHolder<ShapedMaterialRecipe> holder) {
    return inputs(holder.value()).stream().map(Ingredient::display).toList();
  }

  @Override
  public int getWidth(RecipeHolder<ShapedMaterialRecipe> holder) {
    return holder.value().getWidth();
  }

  @Override
  public int getHeight(RecipeHolder<ShapedMaterialRecipe> holder) {
    return holder.value().getHeight();
  }

  @Override
  public void setRecipe(RecipeHolder<ShapedMaterialRecipe> holder, IRecipeLayoutBuilder builder,
                        ICraftingGridHelper craftingGridHelper, IFocusGroup focuses) {
    ShapedMaterialRecipe recipe = holder.value();
    ItemStack plainResult = recipe.assemble(CraftingInput.EMPTY);
    MaterialValueIngredient materials = recipe.getMaterial();
    List<ItemStack> results = materials == null ? List.of(plainResult) : MaterialRecipeCache.getAllRecipes().stream()
      .filter(materials::test)
      .flatMap(material -> {
        ItemStack stack = plainResult.copy();
        recipe.setMaterial(stack, material.getMaterial().getVariant());
        return IntStream.range(0, TinkerIngredients.getItems(material.getIngredient()).length).mapToObj(i -> stack);
      }).toList();
    List<Ingredient> inputs = inputs(recipe);
    int[] materialSlots = IntStream.range(0, inputs.size())
      .filter(i -> MaterialValueIngredient.from(inputs.get(i)) != null).toArray();
    MaterialsCraftingExtension.setRecipe(builder, craftingGridHelper, inputs, results, plainResult,
      recipe.getWidth(), recipe.getHeight(), materialSlots);
  }
}
