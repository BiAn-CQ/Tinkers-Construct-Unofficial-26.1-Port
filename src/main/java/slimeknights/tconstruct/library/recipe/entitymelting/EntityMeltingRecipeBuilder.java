package slimeknights.tconstruct.library.recipe.entitymelting;


import lombok.RequiredArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.mantle.recipe.ingredient.EntityIngredient;

import java.util.function.Consumer;

/** Builder for entity melting recipes */
public class EntityMeltingRecipeBuilder extends AbstractRecipeBuilder<EntityMeltingRecipeBuilder> {
  private final EntityIngredient ingredient;
  private final FluidOutput output;
  private final int damage;

  private EntityMeltingRecipeBuilder(EntityIngredient ingredient, FluidOutput output, int damage) {
    this.ingredient = ingredient;
    this.output = output;
    this.damage = damage;
  }

  /** Creates a builder from the component-aware fluid output. */
  public static EntityMeltingRecipeBuilder melting(EntityIngredient ingredient, FluidOutput output, int damage) {
    return new EntityMeltingRecipeBuilder(ingredient, output, damage);
  }

  /** Creates a new builder */
  public static EntityMeltingRecipeBuilder melting(EntityIngredient ingredient, FluidStack output, int damage) {
    return melting(ingredient, FluidOutput.fromStack(output), damage);
  }

  /** Creates a new builder doing 2 damage */
  public static EntityMeltingRecipeBuilder melting(EntityIngredient ingredient, FluidOutput output) {
    return melting(ingredient, output, 2);
  }

  /** Creates a new builder doing 2 damage */
  public static EntityMeltingRecipeBuilder melting(EntityIngredient ingredient, FluidStack output) {
    return melting(ingredient, output, 2);
  }

  @Override
  public void save(RecipeOutput consumer) {
    save(consumer, BuiltInRegistries.FLUID.getKey(output.get().getFluid()));
  }

  @Override
  public void save(RecipeOutput consumer, Identifier id) {
    var advancementId = this.buildOptionalAdvancement(id, "entity_melting");
    saveRecipe(consumer, id, new EntityMeltingRecipe(id, ingredient, output, damage), advancementId);
  }
}
