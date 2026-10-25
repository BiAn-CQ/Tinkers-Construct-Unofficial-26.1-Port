package slimeknights.tconstruct.library.recipe.casting.material;


import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicate;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/** Builder for a composite part recipe, should exist for each part */
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "composite")
public class CompositeCastingRecipeBuilder extends AbstractRecipeBuilder<CompositeCastingRecipeBuilder> {
  private final IMaterialItem result;
  private final int itemCost;
  @Setter @Nullable
  private MaterialStatsId castingStatConflict = null;
  private final RecipeType<?> recipeType;
  private final RecipeSerializer<? extends CompositeCastingRecipe> serializer;
  @Setter
  private IJsonPredicate<MaterialVariantId> allowedMaterials = MaterialPredicate.ANY;

  public static CompositeCastingRecipeBuilder basin(IMaterialItem result, int itemCost) {
    return composite(result, itemCost, TinkerRecipeTypes.CASTING_BASIN.get(), TinkerSmeltery.basinCompositeSerializer.get());
  }

  public static CompositeCastingRecipeBuilder table(IMaterialItem result, int itemCost) {
    return composite(result, itemCost, TinkerRecipeTypes.CASTING_TABLE.get(), TinkerSmeltery.tableCompositeSerializer.get());
  }

  @Override
  public void save(RecipeOutput consumer) {
    save(consumer, BuiltInRegistries.ITEM.getKey(result.asItem()));
  }

  @Override
  public void save(RecipeOutput consumer, Identifier id) {
    var advancementId = this.buildOptionalAdvancement(id, "casting");
    saveRecipe(consumer, id, new CompositeCastingRecipe(recipeType, serializer, id, group, itemCost, result, allowedMaterials, castingStatConflict), advancementId);
  }

}
