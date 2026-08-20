package slimeknights.tconstruct.library.recipe;

import net.minecraft.core.HolderSet;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.recipe.ingredient.ItemTagIngredient;
import slimeknights.mantle.recipe.ingredient.OrIngredient;
import slimeknights.tconstruct.library.recipe.ingredient.MaterialIngredient;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** Small 26.1 compatibility helpers for the vanilla Ingredient factory changes. */
public final class TinkerIngredients {
  /** Replacement for the removed vanilla {@code Ingredient.EMPTY} singleton. */
  public static final Ingredient EMPTY = IngredientLoadable.EMPTY_INGREDIENT;

  private TinkerIngredients() {}

  public static Ingredient of(ItemLike item) {
    return Ingredient.of(item);
  }

  public static Ingredient of(ItemLike... items) {
    return or(Arrays.stream(items).map(Ingredient::of).toList());
  }

  public static Ingredient of(ItemStack... stacks) {
    return or(Arrays.stream(stacks).map(stack -> Ingredient.of(stack.getItem())).toList());
  }

  public static Ingredient of(Stream<? extends ItemLike> items) {
    return or(items.map(Ingredient::of).toList());
  }

  public static Ingredient of(HolderSet<Item> items) {
    return items.unwrapKey().<Ingredient>map(ItemTagIngredient::of)
      .orElseGet(() -> or(items.stream().map(holder -> Ingredient.of(holder.value())).toList()));
  }

  /** Replacement for the removed {@code Ingredient.of(TagKey<Item>)} overload. */
  public static Ingredient of(TagKey<Item> tag) {
    return ItemTagIngredient.of(tag);
  }

  private static Ingredient or(List<Ingredient> ingredients) {
    return switch (ingredients.size()) {
      case 0 -> EMPTY;
      case 1 -> ingredients.getFirst();
      default -> OrIngredient.of(ingredients);
    };
  }

  /**
   * Tests an ingredient while preserving the 1.20.1 meaning of the removed
   * {@code Ingredient.EMPTY}: it matches an empty item stack.
   */
  public static boolean matches(Ingredient ingredient, ItemStack stack) {
    return ingredient == EMPTY ? stack.isEmpty() : ingredient.test(stack);
  }

  /** Materializes an ingredient for legacy display code that still expects item stacks. */
  public static ItemStack[] getItems(Ingredient ingredient) {
    if (ingredient == EMPTY) {
      return new ItemStack[0];
    }
    if (ingredient.getCustomIngredient() instanceof MaterialIngredient material) {
      return material.getItems();
    }
    return resolveItems(ingredient).map(ItemStack::new).toArray(ItemStack[]::new);
  }

  public static List<ItemStack> getItemList(Ingredient ingredient) {
    if (ingredient == EMPTY) {
      return List.of();
    }
    if (ingredient.getCustomIngredient() instanceof MaterialIngredient material) {
      return List.of(material.getItems());
    }
    return resolveItems(ingredient).map(ItemStack::new).toList();
  }

  /**
   * Reads a tag-backed ingredient without forcing the lazy 26.1 named holder set
   * through {@link Ingredient#items()}, which intentionally rejects construction-time
   * tag dereferences.  The registry is bound by the time JEI asks for display stacks.
   */
  private static java.util.stream.Stream<Holder<Item>> resolveItems(Ingredient ingredient) {
    try {
      var tag = ingredient.getValues().unwrapKey();
      if (tag.isPresent()) {
        return StreamSupport.stream(BuiltInRegistries.ITEM.getTagOrEmpty(tag.get()).spliterator(), false);
      }
    } catch (IllegalStateException ignored) {
      // Custom ingredients do not expose a vanilla HolderSet; use their item stream below.
    }
    return ingredient.items();
  }
}
