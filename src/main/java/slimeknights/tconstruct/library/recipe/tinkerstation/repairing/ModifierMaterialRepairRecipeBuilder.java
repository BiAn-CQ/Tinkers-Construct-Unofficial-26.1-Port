package slimeknights.tconstruct.library.recipe.tinkerstation.repairing;


import lombok.RequiredArgsConstructor;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.util.LazyModifier;

import java.util.function.Consumer;

/** @deprecated use {@link slimeknights.tconstruct.library.modifiers.modules.behavior.MaterialRepairModule} */
@Deprecated(forRemoval = true)
@RequiredArgsConstructor(staticName = "repair")
public class ModifierMaterialRepairRecipeBuilder extends AbstractRecipeBuilder<ModifierMaterialRepairRecipeBuilder> {
  private final ModifierId modifier;
  private final MaterialId material;
  private final MaterialStatsId statType;

  public static ModifierMaterialRepairRecipeBuilder repair(LazyModifier modifier, MaterialId material, MaterialStatsId statType) {
    return repair(modifier.getId(), material, statType);
  }

  @Override
  public void save(RecipeOutput consumer) {
    save(consumer, modifier.location());
  }

  /** Builds the recipe for the crafting table using a repair kit */
  @SuppressWarnings("removal")
  public ModifierMaterialRepairRecipeBuilder saveCraftingTable(RecipeOutput consumer, Identifier id) {
    var advancementId = buildOptionalAdvancement(id, "tinker_station");
    saveRecipe(consumer, id, new ModifierMaterialRepairKitRecipe(id, modifier, material, statType), advancementId);
    return this;
  }

  @SuppressWarnings("removal")
  @Override
  public void save(RecipeOutput consumer, Identifier id) {
    var advancementId = buildOptionalAdvancement(id, "tinker_station");
    saveRecipe(consumer, id, new ModifierMaterialRepairRecipe(id, modifier, material, statType), advancementId);
  }
}
