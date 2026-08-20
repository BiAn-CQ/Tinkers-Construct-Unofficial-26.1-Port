package slimeknights.tconstruct.library.recipe;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.ingredient.MaterialIngredient;

/** Forwarding factories for material-aware item ingredients. */
public final class MaterialTinkerIngredients {
  private MaterialTinkerIngredients() {}

  public static Ingredient of(ItemLike item) {
    return MaterialIngredient.of(item);
  }

  public static Ingredient of(ItemLike item, IJsonPredicate<MaterialVariantId> predicate) {
    return MaterialIngredient.of(item, predicate);
  }

  public static Ingredient of(Ingredient ingredient) {
    return MaterialIngredient.of(ingredient);
  }

  public static Ingredient of(Ingredient ingredient, IJsonPredicate<MaterialVariantId> predicate) {
    return MaterialIngredient.of(ingredient, predicate);
  }

  public static Ingredient of(ItemLike item, MaterialVariantId material) {
    return MaterialIngredient.of(item, material);
  }

  public static Ingredient of(Ingredient ingredient, MaterialVariantId material) {
    return MaterialIngredient.of(ingredient, material);
  }

  public static Ingredient of(TagKey<Item> tag, MaterialVariantId material) {
    return MaterialIngredient.of(tag, material);
  }

  public static Ingredient of(ItemLike item, TagKey<IMaterial> tag) {
    return MaterialIngredient.of(item, tag);
  }

  public static Ingredient of(Ingredient ingredient, TagKey<IMaterial> tag) {
    return MaterialIngredient.of(ingredient, tag);
  }

  public static Ingredient of(TagKey<Item> tag) {
    return MaterialIngredient.of(tag);
  }
}
