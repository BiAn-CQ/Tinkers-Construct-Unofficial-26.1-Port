package slimeknights.tconstruct.library.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.recipe.helper.LoadableIngredientSerializer;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicate;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicateField;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipe;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

/** Ingredient matching material items with a value in a configured range. */
public final class MaterialValueIngredient implements ICustomIngredient {
  public static final Identifier ID = TConstruct.getResource("material_value");
  private static final MaterialPredicateField<MaterialValueIngredient> MATERIAL_FIELD = new MaterialPredicateField<>("material", ingredient -> ingredient.material);
  public static final IngredientType<MaterialValueIngredient> TYPE = new IngredientType<>(
    LoadableIngredientSerializer.mapCodec(MaterialValueIngredient::parse, MaterialValueIngredient::serialize));

  private final IJsonPredicate<MaterialVariantId> material;
  private final float minValue;
  private final float maxValue;
  @Nullable
  private ItemStack[] items;

  private MaterialValueIngredient(IJsonPredicate<MaterialVariantId> material, float minValue, float maxValue) {
    this.material = material;
    this.minValue = minValue;
    this.maxValue = maxValue;
  }

  private static MaterialValueIngredient parse(JsonObject json) {
    float minValue;
    float maxValue;
    JsonElement value = json.get("value");
    if (value.isJsonPrimitive()) {
      minValue = maxValue = value.getAsFloat();
    } else {
      JsonObject object = GsonHelper.convertToJsonObject(value, "value");
      minValue = GsonHelper.getAsFloat(object, "min", 0);
      maxValue = GsonHelper.getAsFloat(object, "max", Float.POSITIVE_INFINITY);
    }
    return new MaterialValueIngredient(MATERIAL_FIELD.get(json), minValue, maxValue);
  }

  private static JsonObject serialize(MaterialValueIngredient ingredient) {
    JsonObject json = new JsonObject();
    MATERIAL_FIELD.serialize(ingredient, json);
    if (ingredient.minValue == ingredient.maxValue) {
      json.addProperty("value", ingredient.minValue);
    } else {
      JsonObject value = new JsonObject();
      if (ingredient.minValue > 0) value.addProperty("min", ingredient.minValue);
      if (Float.isFinite(ingredient.maxValue)) value.addProperty("max", ingredient.maxValue);
      json.add("value", value);
    }
    return json;
  }

  public static Ingredient of(IJsonPredicate<MaterialVariantId> materials, float minValue, float maxValue) {
    return new MaterialValueIngredient(materials, minValue, maxValue).toVanilla();
  }

  public static Ingredient of(IJsonPredicate<MaterialVariantId> materials, float value) {
    return of(materials, value, value);
  }

  @Nullable
  public static MaterialValueIngredient from(Ingredient ingredient) {
    return ingredient.getCustomIngredient() instanceof MaterialValueIngredient material ? material : null;
  }

  public boolean test(MaterialRecipe recipe) {
    float value = recipe.getValue() / (float)recipe.getNeeded();
    return minValue <= value && value <= maxValue && material.matches(recipe.getMaterial().getVariant());
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    if (stack == null) return false;
    MaterialRecipe recipe = MaterialRecipeCache.findRecipe(stack);
    return recipe != MaterialRecipe.EMPTY && test(recipe);
  }

  @Override
  public Stream<Holder<net.minecraft.world.item.Item>> items() {
    if (items == null) {
      if (MaterialRecipeCache.getAllRecipes().isEmpty()) {
        // Ingredient caches the first returned stream. A placeholder here
        // would survive after material recipes finish loading, so fail this
        // access and leave the cache unset instead.
        throw new IllegalStateException("Material value candidates requested before material recipes were loaded");
      }
      ItemStack[] resolved = MaterialRecipeCache.getAllRecipes().stream()
        .filter(this::test)
        .flatMap(recipe -> recipe.getIngredient().items().map(holder -> new ItemStack(holder.value())))
        .toArray(ItemStack[]::new);
      if (resolved.length == 0) {
        // A fully loaded predicate with no matches remains visible as the
        // established diagnostic barrier instead of invalidating its recipe.
        return Stream.of(BuiltInRegistries.ITEM.wrapAsHolder(Items.BARRIER));
      }
      items = resolved;
    }
    return Arrays.stream(items).map(stack -> BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem()));
  }

  private boolean contains(MaterialValueIngredient other) {
    return minValue <= other.minValue && other.maxValue <= maxValue;
  }

  public MaterialValueIngredient merge(MaterialValueIngredient other) {
    if (this == other) return this;
    IJsonPredicate<MaterialVariantId> predicate = material;
    if (material.equals(other.material)) {
      if (contains(other)) return this;
      if (other.contains(this)) return other;
    } else {
      predicate = MaterialPredicate.or(material, other.material);
    }
    return new MaterialValueIngredient(predicate, Math.min(minValue, other.minValue), Math.max(maxValue, other.maxValue));
  }

  @Nullable
  public MaterialVariantId getMaterial(ItemStack stack) {
    MaterialRecipe recipe = MaterialRecipeCache.findRecipe(stack);
    return recipe != MaterialRecipe.EMPTY && test(recipe) ? recipe.getMaterial().getVariant() : null;
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public IngredientType<?> getType() {
    return TYPE;
  }

  @Override
  public boolean equals(Object object) {
    return this == object || object instanceof MaterialValueIngredient other
      && Float.compare(minValue, other.minValue) == 0
      && Float.compare(maxValue, other.maxValue) == 0
      && material.equals(other.material);
  }

  @Override
  public int hashCode() {
    return Objects.hash(material, minValue, maxValue);
  }
}
