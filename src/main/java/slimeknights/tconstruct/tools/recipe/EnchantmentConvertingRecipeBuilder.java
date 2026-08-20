package slimeknights.tconstruct.tools.recipe;


import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.predicate.modifier.ModifierPredicate;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.worktable.AbstractSizedIngredientRecipeBuilder;

import java.util.function.Consumer;

/** Builder for an enchantment converting recipe */
public class EnchantmentConvertingRecipeBuilder extends AbstractSizedIngredientRecipeBuilder<EnchantmentConvertingRecipeBuilder> {
  private final String name;
  private final boolean matchBook;
  private boolean returnInput = false;
  @Setter
  @Accessors(fluent = true)
  private IJsonPredicate<ModifierId> modifierPredicate = ModifierPredicate.ANY;

  private EnchantmentConvertingRecipeBuilder(String name, boolean matchBook) {
    this.name = name;
    this.matchBook = matchBook;
  }

  public static EnchantmentConvertingRecipeBuilder converting(String name, boolean matchBook) {
    return new EnchantmentConvertingRecipeBuilder(name, matchBook);
  }

  /**
   * If true, returns the unenchanted form of the item as an extra result
   */
  public EnchantmentConvertingRecipeBuilder returnInput() {
    returnInput = true;
    return this;
  }

  @Override
  public void save(RecipeOutput consumer) {
    save(consumer, TConstruct.getResource(name));
  }

  @Override
  public void save(RecipeOutput consumer, Identifier id) {
    if (inputs.isEmpty()) {
      throw new IllegalStateException("Must have at least one input");
    }
    var advancementId = buildOptionalAdvancement(id, "modifiers");
    saveRecipe(consumer, id, new EnchantmentConvertingRecipe(id, name, inputs, matchBook, returnInput, modifierPredicate), advancementId);
  }
}
