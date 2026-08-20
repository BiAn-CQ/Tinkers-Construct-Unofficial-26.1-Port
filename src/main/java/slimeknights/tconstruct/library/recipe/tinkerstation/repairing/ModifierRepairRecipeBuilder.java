package slimeknights.tconstruct.library.recipe.tinkerstation.repairing;


import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.util.LazyModifier;

import java.util.function.Consumer;

/** Builds a recipe to repair a tool using a modifier */
public class ModifierRepairRecipeBuilder extends AbstractRecipeBuilder<ModifierRepairRecipeBuilder> {
  private final ModifierId modifier;
  private final Ingredient ingredient;
  private final int repairAmount;

  private ModifierRepairRecipeBuilder(ModifierId modifier, Ingredient ingredient, int repairAmount) {
    this.modifier = modifier;
    this.ingredient = ingredient;
    this.repairAmount = repairAmount;
  }

  public static ModifierRepairRecipeBuilder repair(ModifierId modifier, Ingredient ingredient, int repairAmount) {
    return new ModifierRepairRecipeBuilder(modifier, ingredient, repairAmount);
  }

  public static ModifierRepairRecipeBuilder repair(LazyModifier modifier, Ingredient ingredient, int repairAmount) {
    return repair(modifier.getId(), ingredient, repairAmount);
  }

  @Override
  public void save(RecipeOutput consumer) {
    save(consumer, modifier.location());
  }

  /** Builds the recipe for the crafting table using a repair kit */
  public ModifierRepairRecipeBuilder buildCraftingTable(RecipeOutput consumer, Identifier id) {
    var advancementId = buildOptionalAdvancement(id, "tinker_station");
    saveRecipe(consumer, id, new ModifierRepairCraftingRecipe(id, modifier, ingredient, repairAmount), advancementId);
    return this;
  }

  @Override
  public void save(RecipeOutput consumer, Identifier id) {
    var advancementId = buildOptionalAdvancement(id, "tinker_station");
    saveRecipe(consumer, id, new ModifierRepairTinkerStationRecipe(id, modifier, ingredient, repairAmount), advancementId);
  }
}
