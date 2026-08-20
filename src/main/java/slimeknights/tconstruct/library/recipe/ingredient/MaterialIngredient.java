package slimeknights.tconstruct.library.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.recipe.helper.LoadableIngredientSerializer;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.TinkerLoadables;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicate;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicateField;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;
import slimeknights.tconstruct.library.recipe.TinkerIngredients;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** Ingredient that matches material items and optionally filters their material. */
public final class MaterialIngredient implements ICustomIngredient {
  public static final Identifier ID = TConstruct.getResource("material");
  private static final MaterialPredicateField<MaterialIngredient> MATERIAL_FIELD = new MaterialPredicateField<>("material", ingredient -> ingredient.material);
  public static final IngredientType<MaterialIngredient> TYPE = new IngredientType<>(
    LoadableIngredientSerializer.mapCodec(MaterialIngredient::parse, MaterialIngredient::serialize));

  private final Ingredient nested;
  private final IJsonPredicate<MaterialVariantId> material;
  @Nullable
  private ItemStack[] materialStacks;

  private MaterialIngredient(Ingredient nested, IJsonPredicate<MaterialVariantId> material) {
    this.nested = nested;
    this.material = material;
  }

  private static MaterialIngredient parse(JsonObject json) {
    /*
     * 1.20.1 serialized this ingredient using an `item` member, while the
     * native 26.1 custom ingredient shape uses `match`.  Keep accepting the
     * old form, but route it through Mantle's registry-aware ingredient
     * loader so compact 26.1 holder-set syntax and tags are handled too.
     */
    JsonElement match = json.get("match");
    Ingredient nested;
    if (match == null && json.has("item")) {
      Identifier itemId = Identifier.tryParse(json.get("item").getAsString());
      Item item = itemId == null ? null : BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
      if (item == null) {
        throw new JsonParseException("Unknown item in material ingredient: " + json.get("item"));
      }
      nested = Ingredient.of(item);
    } else if (match != null) {
      nested = IngredientLoadable.DISALLOW_EMPTY.convert(match, "match", TypedMap.empty());
    } else {
      throw new JsonParseException("Missing match or item in material ingredient");
    }
    IJsonPredicate<MaterialVariantId> material = MATERIAL_FIELD.get(json);
    if (json.has("tag")) {
      TConstruct.LOG.warn("Using deprecated tag field on material ingredient");
      IJsonPredicate<MaterialVariantId> tagPredicate = MaterialPredicate.tag(TinkerLoadables.MATERIAL_TAGS.getIfPresent(json, "tag"));
      material = material == MaterialPredicate.ANY ? tagPredicate : MaterialPredicate.and(material, tagPredicate);
    }
    return new MaterialIngredient(nested, material);
  }

  private static JsonObject serialize(MaterialIngredient ingredient) {
    JsonObject json = new JsonObject();
    json.add("match", JsonHelper.serialize(Ingredient.CODEC, ingredient.nested));
    MATERIAL_FIELD.serialize(ingredient, json);
    return json;
  }

  private static IJsonPredicate<MaterialVariantId> makePredicate(MaterialVariantId material, @Nullable TagKey<IMaterial> tag) {
    IJsonPredicate<MaterialVariantId> predicate = material.equals(IMaterial.UNKNOWN.getIdentifier()) ? MaterialPredicate.ANY : MaterialPredicate.variant(material);
    if (tag != null) {
      IJsonPredicate<MaterialVariantId> tagPredicate = MaterialPredicate.tag(tag);
      predicate = predicate == MaterialPredicate.ANY ? tagPredicate : MaterialPredicate.and(predicate, tagPredicate);
    }
    return predicate;
  }

  private static Ingredient wrap(Ingredient nested, IJsonPredicate<MaterialVariantId> material) {
    return new MaterialIngredient(nested, material).toVanilla();
  }

  public static Ingredient of(Ingredient ingredient, IJsonPredicate<MaterialVariantId> material) { return wrap(ingredient, material); }
  public static Ingredient of(ItemLike item, IJsonPredicate<MaterialVariantId> material) { return wrap(TinkerIngredients.of(item), material); }
  public static Ingredient of(Ingredient ingredient) { return wrap(ingredient, MaterialPredicate.ANY); }
  public static Ingredient of(Ingredient ingredient, MaterialVariantId material) { return wrap(ingredient, MaterialPredicate.variant(material)); }
  public static Ingredient of(Ingredient ingredient, TagKey<IMaterial> tag) { return wrap(ingredient, MaterialPredicate.tag(tag)); }
  public static Ingredient of(ItemLike item, MaterialVariantId material) { return wrap(TinkerIngredients.of(item), MaterialPredicate.variant(material)); }
  public static Ingredient of(ItemLike item, TagKey<IMaterial> tag) { return wrap(TinkerIngredients.of(item), MaterialPredicate.tag(tag)); }
  public static Ingredient of(ItemLike item) { return wrap(TinkerIngredients.of(item), MaterialPredicate.ANY); }
  public static Ingredient of(TagKey<Item> tag, MaterialVariantId material) { return wrap(TinkerIngredients.of(tag), MaterialPredicate.variant(material)); }
  public static Ingredient of(TagKey<Item> tag) { return wrap(TinkerIngredients.of(tag), MaterialPredicate.ANY); }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    return stack != null && !stack.isEmpty() && nested.test(stack)
      && (material == MaterialPredicate.ANY || material.matches(IMaterialItem.getMaterialFromStack(stack)));
  }

  @Override
  public Stream<Holder<Item>> items() {
    // Vanilla calls this while recipes are decoded, before item components are
    // bound. Material variants only affect stack components, not the candidate
    // item types, so keep this path holder-only and leave component-bearing
    // stacks to getItems() for recipe viewers after loading completes.
    // Ingredient caches the first successfully returned stream forever. Do
    // not turn a transient unbound-holder failure into a permanently empty
    // candidate list; propagating leaves the vanilla cache unset and makes
    // the actual ordering problem visible to the resource reload.
    return nested.items();
  }

  /**
   * Returns the component-bearing stacks used by recipe viewers. The 26.1
   * {@link ICustomIngredient#items()} contract only exposes item holders, so
   * converting that stream back to stacks loses the selected material.
   */
  public ItemStack[] getItems() {
    if (materialStacks == null) {
      if (!MaterialRegistry.isFullyLoaded()) {
        try {
          return TinkerIngredients.getItems(nested);
        } catch (UnsupportedOperationException exception) {
          return new ItemStack[0];
        }
      }
      try {
        ItemStack[] resolved = Arrays.stream(TinkerIngredients.getItems(nested))
          .flatMap(stack -> MaterialRecipeCache.getAllVariants().stream()
            .filter(material::matches)
            .map(mat -> IMaterialItem.withMaterial(stack, mat)))
          .distinct().toArray(ItemStack[]::new);
        if (resolved.length == 0) {
          // During server construction the material cache can still be empty.
          // Keep the recipe visible with the nested display candidates and
          // recompute material variants on a later reload.
          ItemStack[] fallback = TinkerIngredients.getItems(nested);
          return fallback.length == 0 ? new ItemStack[] { new ItemStack(Items.BARRIER) } : fallback;
        }
        materialStacks = resolved;
      } catch (UnsupportedOperationException exception) {
        return new ItemStack[] { new ItemStack(Items.BARRIER) };
      }
    }
    return materialStacks;
  }

  @Override
  public boolean isSimple() {
    return material == MaterialPredicate.ANY && nested.isSimple();
  }

  @Override
  public IngredientType<?> getType() {
    return TYPE;
  }

  @Override
  public boolean equals(Object object) {
    return this == object || object instanceof MaterialIngredient other
      && nested.equals(other.nested) && material.equals(other.material);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nested, material);
  }
}
