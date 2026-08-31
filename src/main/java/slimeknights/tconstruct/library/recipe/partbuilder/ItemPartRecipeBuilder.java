package slimeknights.tconstruct.library.recipe.partbuilder;


import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.tconstruct.library.materials.definition.MaterialId;

import java.util.function.Consumer;

@RequiredArgsConstructor(staticName = "item")
public class ItemPartRecipeBuilder extends AbstractRecipeBuilder<ItemPartRecipeBuilder> {
  private final Identifier pattern;
  private final ItemOutput result;
  @Setter @Accessors(chain = true)
  private Ingredient patternItem = IPartBuilderRecipe.DEFAULT_PATTERNS;
  private MaterialId materialId = MaterialId.UNKNOWN;
  private int cost = 0;

  /** Sets the material Id and cost */
  public ItemPartRecipeBuilder material(MaterialId material, int cost) {
    this.materialId = material;
    this.cost = cost;
    return this;
  }

  @Override
  public void save(RecipeOutput consumer) {
    save(consumer, BuiltInRegistries.ITEM.getKey(result.get().getItem()));
  }

  @Override
  public void save(RecipeOutput consumer, Identifier id) {
    var advancementId = buildOptionalAdvancement(id, "parts");
    saveRecipe(consumer, id, new ItemPartRecipe(id, materialId, new Pattern(pattern), patternItem, cost, result), advancementId);
  }
}
