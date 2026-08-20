package slimeknights.tconstruct.fluids.util;

import slimeknights.tconstruct.library.recipe.TinkerIngredients;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.brewing.BrewingRecipe;

/** Recipe for transforming a custom bottle with the same reagent as a vanilla container conversion. */
public class BottleBrewingRecipe extends BrewingRecipe {
  public BottleBrewingRecipe(Ingredient input, Ingredient reagent, ItemStack output) {
    super(input, reagent, output);
  }

  /** Legacy constructor for the two vanilla bottle transitions used by Tinkers. */
  @Deprecated(forRemoval = true)
  public BottleBrewingRecipe(Ingredient input, Item from, Item to, ItemStack output) {
    this(input, TinkerIngredients.of(getVanillaReagent(from, to)), output);
  }

  private static Item getVanillaReagent(Item from, Item to) {
    if (from == net.minecraft.world.item.Items.POTION && to == net.minecraft.world.item.Items.SPLASH_POTION) {
      return net.minecraft.world.item.Items.GUNPOWDER;
    }
    if (from == net.minecraft.world.item.Items.SPLASH_POTION && to == net.minecraft.world.item.Items.LINGERING_POTION) {
      return net.minecraft.world.item.Items.DRAGON_BREATH;
    }
    throw new IllegalArgumentException("Unknown vanilla bottle transition: " + from + " -> " + to);
  }
}
