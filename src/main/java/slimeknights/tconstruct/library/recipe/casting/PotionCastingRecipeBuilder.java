package slimeknights.tconstruct.library.recipe.casting;

import slimeknights.tconstruct.library.recipe.FluidTinkerIngredients;
import slimeknights.tconstruct.library.recipe.TinkerIngredients;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;


import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import javax.annotation.Nullable;
import java.util.function.Consumer;
/**
 * Builder for a potion bottle filling recipe. Takes a fluid and optional cast to create an item that copies the fluid NBT
 */
@SuppressWarnings({"WeakerAccess", "unused", "UnusedReturnValue"})
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PotionCastingRecipeBuilder extends AbstractRecipeBuilder<PotionCastingRecipeBuilder> {
  private final Item result;
  @Nullable
  private final ModifierId modifier;
  private final RecipeType<?> recipeType;
  private final RecipeSerializer<? extends PotionCastingRecipe> recipeSerializer;
  private final boolean clearing;
  private Ingredient bottle = TinkerIngredients.EMPTY;
  private FluidIngredient fluid = FluidTinkerIngredients.EMPTY;
  @Setter @Accessors(chain = true)
  private int coolingTime = 5;

  /* Bottle filling */

  /** Creates a new casting recipe for a bottle */
  public static PotionCastingRecipeBuilder castingRecipe(ItemLike result, RecipeType<?> recipeType, RecipeSerializer<PotionCastingRecipe> serializer) {
    return new PotionCastingRecipeBuilder(result.asItem(), null, recipeType, serializer, false);
  }

  /**
   * Creates a new casting basin recipe
   * @param result  Recipe result
   * @return  Builder instance
   */
  public static PotionCastingRecipeBuilder basinRecipe(ItemLike result) {
    return castingRecipe(result, TinkerRecipeTypes.CASTING_BASIN.get(), TinkerSmeltery.basinPotionRecipeSerializer.get());
  }

  /**
   * Creates a new casting table recipe
   * @param result  Recipe result
   * @return  Builder instance
   */
  public static PotionCastingRecipeBuilder tableRecipe(ItemLike result) {
    return castingRecipe(result, TinkerRecipeTypes.CASTING_TABLE.get(), TinkerSmeltery.tablePotionRecipeSerializer.get());
  }


  /* Modifier casting */

  /** Creates a new casting recipe for a bottle */
  public static PotionCastingRecipeBuilder tippingRecipe(ModifierId modifier, RecipeType<?> recipeType, RecipeSerializer<? extends PotionCastingRecipe> serializer) {
    return new PotionCastingRecipeBuilder(Items.AIR, modifier, recipeType, serializer, false);
  }

  private static PotionCastingRecipeBuilder clearingRecipe(ModifierId modifier, RecipeType<?> recipeType, RecipeSerializer<TipClearingCastingRecipe> serializer) {
    return new PotionCastingRecipeBuilder(Items.AIR, modifier, recipeType, serializer, true);
  }

  /**
   * Creates a new tool potion casting basin recipe
   * @param modifier  Modifier required to cast
   * @return  Builder instance
   */
  public static PotionCastingRecipeBuilder basinTipping(ModifierId modifier) {
    return tippingRecipe(modifier, TinkerRecipeTypes.CASTING_BASIN.get(), TinkerSmeltery.basinTippingRecipeSerializer.get());
  }

  /**
   * Creates a new tool potion casting table recipe
   * @param modifier  Recipe result
   * @return  Builder instance
   */
  public static PotionCastingRecipeBuilder tableTipping(ModifierId modifier) {
    return tippingRecipe(modifier, TinkerRecipeTypes.CASTING_TABLE.get(), TinkerSmeltery.tableTippingRecipeSerializer.get());
  }

  /**
   * Creates a new tool potion casting basin recipe
   * @param modifier  Modifier required to cast
   * @return  Builder instance
   */
  public static PotionCastingRecipeBuilder basinClearing(ModifierId modifier) {
    return clearingRecipe(modifier, TinkerRecipeTypes.CASTING_BASIN.get(), TinkerSmeltery.basinTipClearingRecipeSerializer.get());
  }

  /**
   * Creates a new tool potion casting table recipe
   * @param modifier  Recipe result
   * @return  Builder instance
   */
  public static PotionCastingRecipeBuilder tableClearing(ModifierId modifier) {
    return clearingRecipe(modifier, TinkerRecipeTypes.CASTING_TABLE.get(), TinkerSmeltery.tableTipClearingRecipeSerializer.get());
  }


  /* Fluids */

  /**
   * Sets the fluid for this recipe
   * @param tagIn   Tag<Fluid> instance
   * @param amount  amount of fluid
   * @return  Builder instance
   */
  public PotionCastingRecipeBuilder setFluid(TagKey<Fluid> tagIn, int amount) {
    return this.setFluid(FluidTinkerIngredients.of(tagIn, amount));
  }

  /**
   * Sets the fluid ingredient
   * @param fluid  Fluid ingredient instance
   * @return  Builder instance
   */
  public PotionCastingRecipeBuilder setFluid(FluidIngredient fluid) {
    this.fluid = fluid;
    return this;
  }


  /* Cast */

  /**
   * Sets the cast from a tag, bottles are always consumed
   * @param tagIn     Cast tag
   * @return  Builder instance
   */
  public PotionCastingRecipeBuilder setBottle(TagKey<Item> tagIn) {
    return this.setBottle(TinkerIngredients.of(tagIn));
  }

  /**
   * Sets the bottle from an item, bottles are always consumed
   * @param itemIn    Cast item
   * @return  Builder instance
   */
  public PotionCastingRecipeBuilder setBottle(ItemLike itemIn) {
    return this.setBottle(TinkerIngredients.of(itemIn));
  }

  /**
   * Sets the bottle from an ingredient, bottles are always consumed
   * @param ingredient  Cast ingredient
   * @return  Builder instance
   */
  public PotionCastingRecipeBuilder setBottle(Ingredient ingredient) {
    this.bottle = ingredient;
    return this;
  }

  /**
   * Builds a recipe using the registry name as the recipe name
   * @param consumerIn  Recipe consumer
   */
  @Override
  public void save(RecipeOutput consumerIn) {
    this.save(consumerIn, BuiltInRegistries.ITEM.getKey(this.result));
  }

  @Override
  public void save(RecipeOutput consumer, Identifier id) {
    if (this.fluid == FluidTinkerIngredients.EMPTY) {
      throw new IllegalStateException("Casting recipes require a fluid input");
    }
    if (this.coolingTime < 0) {
      throw new IllegalStateException("Cooling time is too low, must be at least 0");
    }
    var advancementId = this.buildOptionalAdvancement(id, "casting");
    if (modifier != null) {
      PotionCastingRecipe recipe = clearing
        ? new TipClearingCastingRecipe(recipeType, recipeSerializer, id, group, bottle, fluid, coolingTime, modifier)
        : new TippingCastingRecipe(recipeType, recipeSerializer, id, group, bottle, fluid, coolingTime, modifier);
      saveRecipe(consumer, id, recipe, advancementId);
    } else {
      saveRecipe(consumer, id, new PotionCastingRecipe(recipeType, recipeSerializer, id, group, bottle, fluid, result, coolingTime), advancementId);
    }
  }
}
