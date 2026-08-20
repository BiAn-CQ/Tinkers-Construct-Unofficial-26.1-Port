package slimeknights.tconstruct.library.recipe.modifiers.adding;

import slimeknights.tconstruct.library.recipe.TinkerIngredients;


import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.tconstruct.common.TinkerTags;

import java.util.function.Consumer;

/**
 * Builder for overslime recipes
 */
@RequiredArgsConstructor(staticName = "modifier")
public class OverslimeModifierRecipeBuilder extends AbstractRecipeBuilder<OverslimeModifierRecipeBuilder> {
  @Setter @Accessors(chain = true)
  private Ingredient tools = TinkerIngredients.of(TinkerTags.Items.DURABILITY);
  private final Ingredient ingredient;
  private final int restoreAmount;

  /** Creates a new builder for the given item */
  public static OverslimeModifierRecipeBuilder modifier(ItemLike item, int restoreAmount) {
    return modifier(TinkerIngredients.of(item), restoreAmount);
  }

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
    var advancementId = buildOptionalAdvancement(id, "modifiers");
    saveRecipe(consumer, id, new OverslimeModifierRecipe(id, tools, ingredient, restoreAmount), advancementId);
  }

  /** Creates a crafting table overslime repair recipe */
  public OverslimeModifierRecipeBuilder saveCrafting(RecipeOutput consumer, Identifier id) {
    if (ingredient == TinkerIngredients.EMPTY) {
      throw new IllegalStateException("Empty ingredient not allowed");
    }
    var advancementId = buildOptionalAdvancement(id, "modifiers");
    saveRecipe(consumer, id, new OverslimeCraftingTableRecipe(id, tools, ingredient, restoreAmount), advancementId);
    return this;
  }
}
