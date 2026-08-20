package slimeknights.tconstruct.library.recipe.alloying;

import slimeknights.tconstruct.library.recipe.FluidTinkerIngredients;
import slimeknights.tconstruct.library.recipe.TinkerIngredients;


import lombok.RequiredArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe.AlloyIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe.getTemperature;

/** Builder for alloy recipes */
@SuppressWarnings("unused")
public class AlloyRecipeBuilder extends AbstractRecipeBuilder<AlloyRecipeBuilder> {
  private final FluidOutput output;
  private final int temperature;
  private final List<AlloyIngredient> inputs = new ArrayList<>();

  private AlloyRecipeBuilder(FluidOutput output, int temperature) {
    this.output = output;
    this.temperature = temperature;
  }

  /** Creates a builder from the component-aware fluid output. */
  public static AlloyRecipeBuilder alloy(FluidOutput output, int temperature) {
    return new AlloyRecipeBuilder(output, temperature);
  }

  /**
   * Creates a new recipe producing the given fluid
   * @param fluid    fluid to alloy
   * @param amount   fluid amount
   * @return  Builder instance
   */
  public static AlloyRecipeBuilder alloy(FluidObject<?> fluid, int amount) {
    return alloy(fluid.result(amount), getTemperature(fluid));
  }

  /**
   * Creates a new recipe producing the given fluid
   * @param fluid   Fluid output
   * @return  Builder instance
   */
  public static AlloyRecipeBuilder alloy(FluidStack fluid) {
    return alloy(FluidOutput.fromStack(fluid), getTemperature(fluid));
  }

  /**
   * Creates a new recipe producing the given fluid
   * @param fluid   Fluid output
   * @param amount  Output amount
   * @return  Builder instance
   */
  public static AlloyRecipeBuilder alloy(Fluid fluid, int amount) {
    return alloy(new FluidStack(fluid, amount));
  }


  /* Inputs */

  /**
   * Adds an input
   * @param input  Input ingredient
   * @return  Builder instance
   */
  public AlloyRecipeBuilder addInput(FluidIngredient input) {
    inputs.add(new AlloyIngredient(input, false));
    return this;
  }

  /**
   * Adds an input that is not consumed
   * @param input  Catalyst ingredient
   * @return  Builder instance
   */
  public AlloyRecipeBuilder addCatalyst(FluidIngredient input) {
    inputs.add(new AlloyIngredient(input, true));
    return this;
  }

  /**
   * Adds an input
   * @param input  Input fluid
   * @return  Builder instance
   */
  public AlloyRecipeBuilder addInput(FluidStack input) {
    return addInput(FluidTinkerIngredients.of(input));
  }

  /**
   * Adds an input
   * @param fluid   Input fluid
   * @param amount  Input amount
   * @return  Builder instance
   */
  public AlloyRecipeBuilder addInput(Fluid fluid, int amount) {
    return addInput(FluidTinkerIngredients.of(new FluidStack(fluid, amount)));
  }

  /**
   * Adds an input
   * @param tag     Input tag
   * @param amount  Input amount
   * @return  Builder instance
   */
  public AlloyRecipeBuilder addInput(TagKey<Fluid> tag, int amount) {
    return addInput(FluidTinkerIngredients.of(tag, amount));
  }


  /* Building */

  @Override
  public void save(RecipeOutput consumer) {
    save(consumer, BuiltInRegistries.FLUID.getKey(output.get().getFluid()));
  }

  @Override
  public void save(RecipeOutput consumer, Identifier id) {
    if (inputs.size() < 2) {
      throw new IllegalStateException("Invalid alloying recipe " + id + ", must have at least two inputs");
    }
    saveRecipe(consumer, id, new AlloyRecipe(id, inputs, output, temperature), this.buildOptionalAdvancement(id, "alloys"));
  }
}
