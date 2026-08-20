package slimeknights.tconstruct.library.recipe.material;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import lombok.Getter;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.tables.TinkerTables;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Shapeless recipe with a number of {@link slimeknights.tconstruct.library.recipe.ingredient.MaterialIngredient} and
 * {@link slimeknights.tconstruct.library.recipe.ingredient.MaterialValueIngredient} to set the materials of the result.
 */
public class ShapelessMaterialsRecipe extends ShapelessRecipe implements MaterialsCraftingTableRecipe {
  public static final RecipeSerializer<ShapelessMaterialsRecipe> SERIALIZER = TinkerCraftingRecipeSerializer.create(
    ShapelessMaterialsRecipe::deserialize,
    ShapelessMaterialsRecipe::serialize,
    (buffer, recipe) -> {
      ShapelessRecipe.STREAM_CODEC.encode(buffer, recipe.asVanilla());
      buffer.writeByte(recipe.partCount);
      Serializer.MATERIAL_FIELD.encode(buffer, recipe);
    },
    buffer -> {
      ShapelessRecipe recipe = ShapelessRecipe.STREAM_CODEC.decode(buffer);
      return new ShapelessMaterialsRecipe(recipe, buffer.readByte(), Serializer.MATERIAL_FIELD.decode(buffer));
    });

  /** Number of parts to match */
  @Getter
  private final int partCount;
  /** List of additional materials to add beyond the parts */
  @Getter
  private final List<MaterialVariantId> extraMaterials;

  public ShapelessMaterialsRecipe(Identifier id, String group, CraftingBookCategory category, ItemStack result, NonNullList<Ingredient> ingredients, int partCount, List<MaterialVariantId> extraMaterials) {
    this(group, category, result, ingredients, partCount, extraMaterials, true);
  }

  private ShapelessMaterialsRecipe(String group, CraftingBookCategory category, ItemStack result, List<Ingredient> ingredients, int partCount, List<MaterialVariantId> extraMaterials, boolean showNotification) {
    this(group, category, ItemStackTemplate.fromNonEmptyStack(result), ingredients, partCount, extraMaterials, showNotification);
  }

  private ShapelessMaterialsRecipe(String group, CraftingBookCategory category, ItemStackTemplate result, List<Ingredient> ingredients, int partCount, List<MaterialVariantId> extraMaterials, boolean showNotification) {
    super(new Recipe.CommonInfo(showNotification), new CraftingRecipe.CraftingBookInfo(category, group), result, ingredients);
    this.partCount = partCount;
    this.extraMaterials = extraMaterials;
  }

  public ShapelessMaterialsRecipe(ShapelessRecipe recipe, int partCount, List<MaterialVariantId> extraMaterials) {
    this(recipe.group(), recipe.category(), recipe.result(), recipe.placementInfo().ingredients(), partCount, extraMaterials, recipe.showNotification());
  }

  public List<Ingredient> getIngredients() {
    return placementInfo().ingredients();
  }

  private ShapelessRecipe asVanilla() {
    return new ShapelessRecipe(new Recipe.CommonInfo(showNotification()), new CraftingRecipe.CraftingBookInfo(category(), group()),
      result(), getIngredients());
  }

  @Override
  public List<Ingredient> getParts() {
    return getIngredients();
  }

  /** Sets the material for the given stack */
  @Override
  public void setMaterial(ItemStack stack, MaterialVariantId material) {
    ShapedMaterialsRecipe.setMaterial(stack, material, extraMaterials);
  }

  @Override
  public ItemStack assemble(CraftingInput inventory) {
    return ShapedMaterialsRecipe.assemble(super.assemble(inventory), inventory, getIngredients(), partCount, false, extraMaterials);
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public RecipeSerializer getSerializer() {
    return TinkerTables.shapelessMaterialsRecipeSerializer.get();
  }

  public static final class Serializer {
    static final Loadable<List<MaterialVariantId>> EXTRA_MATERIALS = ShapedMaterialsRecipe.Serializer.EXTRA_MATERIALS;
    static final LoadableField<List<MaterialVariantId>,ShapelessMaterialsRecipe> MATERIAL_FIELD = EXTRA_MATERIALS.defaultField("extra_materials", List.of(), r -> r.extraMaterials);

    private static ShapelessMaterialsRecipe fromJson(JsonObject json, com.mojang.serialization.DynamicOps<?> ops) {
      JsonObject normalized = json.deepCopy();
      JsonElement ingredientsElement = normalized.get("ingredients");
      if (ingredientsElement != null && ingredientsElement.isJsonArray()) {
        JsonArray ingredients = new JsonArray();
        for (JsonElement ingredient : ingredientsElement.getAsJsonArray()) {
          ingredients.add(IngredientLoadable.normalizeLegacyIngredient(ingredient));
        }
        normalized.add("ingredients", ingredients);
      }
      JsonElement resultElement = normalized.get("result");
      if (resultElement != null && resultElement.isJsonObject()) {
        JsonObject result = resultElement.getAsJsonObject();
        if (!result.has("id") && result.has("item")) {
          result.add("id", result.remove("item"));
        }
      }
      com.mojang.serialization.DynamicOps<JsonElement> decodeOps = TinkerCraftingRecipeSerializer.registryJsonOps(ops);
      ShapelessRecipe vanilla = ShapelessRecipe.MAP_CODEC.codec().parse(decodeOps, normalized).getOrThrow();
      int parts = GsonHelper.getAsInt(json, "parts");
      if (parts < 1 || parts > vanilla.placementInfo().ingredients().size()) {
        throw new JsonSyntaxException("Parts must be between 1 and the number of ingredients " + vanilla.placementInfo().ingredients().size());
      }
      return new ShapelessMaterialsRecipe(vanilla, parts, MATERIAL_FIELD.get(normalized));
    }

    private static JsonObject toJson(ShapelessMaterialsRecipe recipe, DynamicOps<?> ops) {
      DynamicOps<JsonElement> jsonOps = TinkerCraftingRecipeSerializer.registryJsonOps(ops);
      JsonObject json = ShapelessRecipe.MAP_CODEC.codec().encodeStart(jsonOps, recipe.asVanilla()).getOrThrow().getAsJsonObject();
      json.addProperty("parts", recipe.partCount);
      MATERIAL_FIELD.serialize(recipe, json);
      return json;
    }
  }

  private static ShapelessMaterialsRecipe deserialize(JsonObject json, com.mojang.serialization.DynamicOps<?> ops) {
    return Serializer.fromJson(json, ops);
  }

  private static JsonObject serialize(ShapelessMaterialsRecipe recipe, DynamicOps<?> ops) {
    return Serializer.toJson(recipe, ops);
  }
}
