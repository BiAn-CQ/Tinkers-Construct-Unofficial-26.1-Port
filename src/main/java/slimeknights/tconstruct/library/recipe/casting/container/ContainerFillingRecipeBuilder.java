package slimeknights.tconstruct.library.recipe.casting.container;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;

/**
 * Builder for a container filling recipe. Takes an arbitrary fluid for a specific amount to fill a native fluid resource handler.
 */
@AllArgsConstructor(staticName = "castingRecipe")
@SuppressWarnings({"WeakerAccess", "unused"})
public class ContainerFillingRecipeBuilder extends AbstractRecipeBuilder<ContainerFillingRecipeBuilder> {
  private final Identifier result;
  private final int fluidAmount;
  private final RecipeType<?> recipeType;
  private final RecipeSerializer<? extends ContainerFillingRecipe> recipeSerializer;

  /**
   * Creates a new builder instance using the given result, amount, and serializer
   * @param result            Recipe result
   * @param fluidAmount       Container size
   * @param recipeSerializer  Serializer
   * @return  Builder instance
   */
  public static ContainerFillingRecipeBuilder castingRecipe(ItemLike result, int fluidAmount, RecipeType<?> recipeType, RecipeSerializer<? extends ContainerFillingRecipe> recipeSerializer) {
    return new ContainerFillingRecipeBuilder(BuiltInRegistries.ITEM.getKey(result.asItem()), fluidAmount, recipeType, recipeSerializer);
  }

  /**
   * Creates a new basin recipe builder using the given result, amount, and serializer
   * @param result            Recipe result
   * @param fluidAmount       Container size
   * @return  Builder instance
   */
  public static ContainerFillingRecipeBuilder basinRecipe(Identifier result, int fluidAmount) {
    return castingRecipe(result, fluidAmount, TinkerRecipeTypes.CASTING_BASIN.get(), TinkerSmeltery.basinFillingRecipeSerializer.get());
  }

  /**
   * Creates a new basin recipe builder using the given result, amount, and serializer
   * @param result            Recipe result
   * @param fluidAmount       Container size
   * @return  Builder instance
   */
  public static ContainerFillingRecipeBuilder basinRecipe(ItemLike result, int fluidAmount) {
    return castingRecipe(result, fluidAmount, TinkerRecipeTypes.CASTING_BASIN.get(), TinkerSmeltery.basinFillingRecipeSerializer.get());
  }

  /**
   * Creates a new table recipe builder using the given result, amount, and serializer
   * @param result            Recipe result
   * @param fluidAmount       Container size
   * @return  Builder instance
   */
  public static ContainerFillingRecipeBuilder tableRecipe(Identifier result, int fluidAmount) {
    return castingRecipe(result, fluidAmount, TinkerRecipeTypes.CASTING_TABLE.get(), TinkerSmeltery.tableFillingRecipeSerializer.get());
  }

  /**
   * Creates a new table recipe builder using the given result, amount, and serializer
   * @param result            Recipe result
   * @param fluidAmount       Container size
   * @return  Builder instance
   */
  public static ContainerFillingRecipeBuilder tableRecipe(ItemLike result, int fluidAmount) {
    return castingRecipe(result, fluidAmount, TinkerRecipeTypes.CASTING_TABLE.get(), TinkerSmeltery.tableFillingRecipeSerializer.get());
  }

  @Override
  public void save(RecipeOutput consumer) {
    this.save(consumer, this.result);
  }

  @Override
  public void save(RecipeOutput consumerIn, Identifier id) {
    var advancementId = this.buildOptionalAdvancement(id, "casting");
    saveRecipe(consumerIn, id, new ContainerFillingRecipe(
      recipeType, recipeSerializer, id, group, fluidAmount, BuiltInRegistries.ITEM.getValue(result)), advancementId);
  }
}
