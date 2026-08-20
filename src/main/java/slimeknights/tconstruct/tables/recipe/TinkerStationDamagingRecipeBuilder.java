package slimeknights.tconstruct.tables.recipe;

import slimeknights.tconstruct.library.recipe.TinkerIngredients;


import lombok.RequiredArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;

import java.util.function.Consumer;

/** Builder for tinker station damaging recipes */
@RequiredArgsConstructor(staticName = "damage")
public class TinkerStationDamagingRecipeBuilder extends AbstractRecipeBuilder<TinkerStationDamagingRecipeBuilder> {

  private final Ingredient ingredient;
  private final int damageAmount;

  @Override
  public void save(RecipeOutput consumer) {
    ItemStack[] stacks = slimeknights.tconstruct.library.recipe.TinkerIngredients.getItems(ingredient);
    if (stacks.length == 0) {
      throw new IllegalStateException("Empty ingredient not allowed");
    }
    save(consumer, BuiltInRegistries.ITEM.getKey(stacks[0].getItem()));
  }

  @Override
  public void save(RecipeOutput consumer, Identifier id) {
    if (ingredient == TinkerIngredients.EMPTY) {
      throw new IllegalStateException("Empty ingredient not allowed");
    }
    var advancementId = buildOptionalAdvancement(id, "tinker_station");
    saveRecipe(consumer, id, new TinkerStationDamagingRecipe(id, ingredient, damageAmount), advancementId);
  }
}
