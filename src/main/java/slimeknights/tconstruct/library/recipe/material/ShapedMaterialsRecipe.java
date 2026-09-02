package slimeknights.tconstruct.library.recipe.material;

import slimeknights.tconstruct.library.recipe.TinkerIngredients;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.util.LogicHelper;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.tables.TinkerTables;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;

/**
 * Shaped recipe with a number of {@link slimeknights.tconstruct.library.recipe.ingredient.MaterialIngredient} and
 * {@link slimeknights.tconstruct.library.recipe.ingredient.MaterialValueIngredient} to set the materials of the result.
 */
public class ShapedMaterialsRecipe extends ShapedRecipe implements MaterialsCraftingTableRecipe {
  public static final RecipeSerializer<ShapedMaterialsRecipe> SERIALIZER = TinkerCraftingRecipeSerializer.create(
    (json, ops) -> ShapedMaterialsRecipe.deserialize(json, ops),
    ShapedMaterialsRecipe::serialize,
    (buffer, recipe) -> {
      ShapedRecipe.STREAM_CODEC.encode(buffer, recipe.asVanilla());
      Serializer.MATERIAL_FIELD.encode(buffer, recipe);
      writeParts(buffer, recipe);
    },
    buffer -> {
      ShapedRecipe recipe = ShapedRecipe.STREAM_CODEC.decode(buffer);
      List<MaterialVariantId> extraMaterials = Serializer.MATERIAL_FIELD.decode(buffer);
      int size = buffer.readVarInt();
      List<Ingredient> distinct = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        distinct.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
      }
      List<Ingredient> inputs = recipe.pattern.ingredients().stream().map(optional -> optional.orElse(TinkerIngredients.EMPTY)).toList();
      // The native recipe stream already carries the input pattern. Use the compact part list only.
      int partSize = buffer.readVarInt();
      List<Ingredient> parts = new ArrayList<>(partSize);
      for (int i = 0; i < partSize; i++) {
        parts.add(LogicHelper.getOrDefault(distinct, buffer.readByte(), TinkerIngredients.EMPTY));
      }
      if (distinct.size() == inputs.stream().distinct().count()) {
        // no-op; the native pattern remains authoritative
      }
      return new ShapedMaterialsRecipe(recipe, List.copyOf(parts), extraMaterials);
    });

  /** List of tool parts to search for in the final recipe */
  @Getter
  private final List<Ingredient> parts;
  /**
   * If true, a part may show up multiple times in the inputs, and all copies should match.
   * If false, only the first instance of a part is checked for each input, allowing a tool with the same part multiple times.
   */
  private final boolean checkRepeats;
  /** List of additional materials to add beyond the parts */
  @Getter
  private final List<MaterialVariantId> extraMaterials;
  public ShapedMaterialsRecipe(Identifier id, String group, CraftingBookCategory category, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, boolean showNotification, List<Ingredient> parts, List<MaterialVariantId> extraMaterials) {
    this(id, group, category, pattern(width, height, ingredients), result, showNotification, parts, extraMaterials);
  }

  private ShapedMaterialsRecipe(Identifier id, String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result, boolean showNotification, List<Ingredient> parts, List<MaterialVariantId> extraMaterials) {
    this(id, group, category, pattern, ItemStackTemplate.fromNonEmptyStack(result), showNotification, parts, extraMaterials);
  }

  private ShapedMaterialsRecipe(Identifier id, String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStackTemplate result, boolean showNotification, List<Ingredient> parts, List<MaterialVariantId> extraMaterials) {
    super(new Recipe.CommonInfo(showNotification), new CraftingRecipe.CraftingBookInfo(category, group), pattern, result);
    this.parts = parts;
    this.checkRepeats = parts.stream().unordered().distinct().count() == parts.size();
    this.extraMaterials = extraMaterials;
  }

  public ShapedMaterialsRecipe(ShapedRecipe recipe, List<Ingredient> parts, List<MaterialVariantId> extraMaterials) {
    this(null, recipe.group(), recipe.category(), recipe.pattern,
         recipe.assemble(CraftingInput.EMPTY), recipe.showNotification(), parts, extraMaterials);
  }

  private static ShapedRecipePattern pattern(int width, int height, NonNullList<Ingredient> ingredients) {
    return new ShapedRecipePattern(width, height,
      ingredients.stream().map(ingredient -> ingredient.isEmpty() ? Optional.<Ingredient>empty() : Optional.of(ingredient)).toList(),
      Optional.empty());
  }

  private ShapedRecipe asVanilla() {
    return new ShapedRecipe(new Recipe.CommonInfo(showNotification()), new CraftingRecipe.CraftingBookInfo(category(), group()),
      pattern, ItemStackTemplate.fromNonEmptyStack(super.assemble(CraftingInput.EMPTY)));
  }

  /** Shaped inputs with empty slots represented by {@link TinkerIngredients#EMPTY}. */
  public List<Ingredient> getInputIngredients() {
    return getIngredients().stream().map(optional -> optional.orElse(TinkerIngredients.EMPTY)).toList();
  }

  /** Copies this recipe with a component-updated result while retaining its packed pattern and material contract. */
  public ShapedMaterialsRecipe withResult(ItemStack result) {
    return new ShapedMaterialsRecipe(null, group(), category(), pattern, result, showNotification(), parts, extraMaterials);
  }

  @Override
  public int getPartCount() {
    return parts.size();
  }

  /**
   * Finds materials for each of the parts
   * @return Array of all matched materials. Array will have no null entries, though the array may be null if no match was found.
   */
  @Nullable
  static MaterialVariantId[] findMaterials(CraftingInput inventory, List<Ingredient> parts, int partCount, boolean checkRepeats) {
    // want one material for each
    MaterialVariantId[] materials = new MaterialVariantId[partCount];
    for (int i = 0; i < inventory.size(); i++) {
      ItemStack stack = inventory.getItem(i);
      if (!stack.isEmpty()) {
        for (int p = 0; p < partCount; p++) {
          MaterialVariantId current = materials[p];
          // if we have not found the material yet, or repeats are considered the same material, test the ingredient
          if ((current == null || checkRepeats) && parts.get(p).test(stack)) {
            MaterialVariantId matched;
            if (stack.getItem() instanceof IMaterialItem materialItem) {
              matched = materialItem.getMaterial(stack);
            } else {
              matched = MaterialRecipeCache.findRecipe(stack).getMaterial().getVariant();
            }
            // first occurrence? thats our material
            if (current == null) {
              materials[p] = matched;
              break;
            } else if (!current.matchesVariant(matched)) {
              // if same material but different variants, just discard the variant
              if (current.getId().equals(matched.getId())) {
                materials[p] = current.getId();
                break;
              } else {
                // if different materials, no match
                return null;
              }
            }
          }
        }
      }
    }
    // ensure we found all materials needed
    for (int p = 0; p < partCount; p++) {
      if (materials[p] == null) {
        return null;
      }
    }
    return materials;
  }

  @Override
  public boolean matches(CraftingInput inventory, Level level) {
    if (!super.matches(inventory, level)) {
      return false;
    }
    // ensure all part materials matched and we found all parts
    return findMaterials(inventory, parts, parts.size(), checkRepeats) != null;
  }

  /** Common logic to this and {@link ShapedMaterialsRecipe} */
  public static void setMaterial(ItemStack stack, MaterialVariantId material, List<MaterialVariantId> extraMaterials) {
    if (extraMaterials.isEmpty() && stack.getItem() instanceof IMaterialItem materialItem) {
      materialItem.setMaterial(stack, material);
    } else {
      MaterialNBT.Builder builder = MaterialNBT.builder();
      builder.add(material);
      for (MaterialVariantId extraMaterial : extraMaterials) {
        builder.add(extraMaterial);
      }
      ToolStack.from(stack).setMaterials(builder.build());
    }
  }

  /** Sets the material for the given stack */
  @Override
  public void setMaterial(ItemStack stack, MaterialVariantId material) {
    setMaterial(stack, material, extraMaterials);
  }

  /** Assembles the item with material information */
  static ItemStack assemble(ItemStack stack, CraftingInput inventory, List<Ingredient> parts, int partCount, boolean checkRepeats, List<MaterialVariantId> extraMaterials) {
    MaterialVariantId[] materials = findMaterials(inventory, parts, partCount, checkRepeats);
    if (materials != null) {
      // if the result is a tool part, and we only have the one material, set its material
      if (materials.length == 1 && extraMaterials.isEmpty() && stack.getItem() instanceof IMaterialItem materialItem) {
        return materialItem.setMaterial(stack, materials[0]);
      }
      MaterialNBT.Builder builder = MaterialNBT.builder();
      // add each material
      for (MaterialVariantId material : materials) {
        builder.add(material);
      }
      // add extra materials
      builder.add(extraMaterials);
      ToolStack.from(stack).setMaterials(builder.build());
    }
    return stack;
  }

  @Override
  public ItemStack assemble(CraftingInput inventory) {
    return assemble(super.assemble(inventory), inventory, parts, parts.size(), checkRepeats, extraMaterials);
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public RecipeSerializer getSerializer() {
    return TinkerTables.shapedMaterialsRecipeSerializer.get();
  }

  public static final class Serializer {
    static final Loadable<List<MaterialVariantId>> EXTRA_MATERIALS = MaterialVariantId.LOADABLE.list(0);
    static final LoadableField<List<MaterialVariantId>, ShapedMaterialsRecipe> MATERIAL_FIELD = EXTRA_MATERIALS.defaultField("extra_materials", List.of(), r -> r.extraMaterials);

    private static ShapedMaterialsRecipe fromJson(JsonObject json, DynamicOps<?> ops) {
      DynamicOps<JsonElement> decodeOps = TinkerCraftingRecipeSerializer.registryJsonOps(ops);

      // Decode the native pattern data directly so "parts" can retain references to the same ingredient instances as the key.
      String group = GsonHelper.getAsString(json, "group", "");
      CraftingBookCategory category = json.has("category")
        ? CraftingBookCategory.CODEC.parse(decodeOps, json.get("category")).getOrThrow()
        : CraftingBookCategory.MISC;
      ShapedRecipePattern.Data data = ShapedRecipePattern.Data.MAP_CODEC.codec().parse(decodeOps, json).getOrThrow();
      ShapedRecipePattern pattern = ShapedRecipePattern.of(data.key(), data.pattern());
      ItemStackTemplate result = ItemStackTemplate.CODEC.parse(decodeOps, GsonHelper.getAsJsonObject(json, "result")).getOrThrow();
      boolean showNotification = GsonHelper.getAsBoolean(json, "show_notification", true);

      // specific to shaped part recipe, map from a pattern string to the ingredients for each character
      // saves memory by not having separate copies of each, plus simplifies the JSON
      String partPattern = GsonHelper.getAsString(json, "parts");
      List<Ingredient> parts = new ArrayList<>();
      for (int i = 0; i < partPattern.length(); i++) {
        String sym = partPattern.substring(i, i + 1);
        Ingredient ingredient = data.key().get(sym.charAt(0));
        if (ingredient == null) {
          throw new JsonSyntaxException("Parts references symbol '" + sym + "' but it's not defined in the key");
        }
        parts.add(ingredient);
      }
      return new ShapedMaterialsRecipe(null, group, category, pattern, result, showNotification, List.copyOf(parts), MATERIAL_FIELD.get(json));
    }

    private static void writeParts(RegistryFriendlyByteBuf buffer, ShapedMaterialsRecipe recipe) {
      // Empty pattern cells are represented by the unresolved __empty tag. Do not
      // call Ingredient#isEmpty on that tag while the recipe packet is encoded.
      List<Ingredient> inputs = recipe.pattern.ingredients().stream().flatMap(Optional::stream).toList();
      List<Ingredient> distinct = inputs.stream().distinct().toList();
      buffer.writeVarInt(distinct.size());
      for (Ingredient ingredient : distinct) Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
      buffer.writeVarInt(recipe.parts.size());
      for (Ingredient ingredient : recipe.parts) buffer.writeByte(distinct.indexOf(ingredient));
    }

    private static final String SYMBOLS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /** Serializes the vanilla shaped fields using the native key/pattern JSON layout. */
    static JsonObject serializeShapedBase(ShapedRecipe recipe, DynamicOps<?> ops) {
      JsonObject json = new JsonObject();
      DynamicOps<JsonElement> jsonOps = TinkerCraftingRecipeSerializer.registryJsonOps(ops);
      if (!recipe.group().isEmpty()) {
        json.addProperty("group", recipe.group());
      }
      if (recipe.category() != CraftingBookCategory.MISC) {
        json.addProperty("category", recipe.category().getSerializedName());
      }

      IdentityHashMap<Ingredient,Character> symbols = new IdentityHashMap<>();
      JsonObject key = new JsonObject();
      com.google.gson.JsonArray pattern = new com.google.gson.JsonArray();
      List<Optional<Ingredient>> ingredients = recipe.pattern.ingredients();
      int nextSymbol = 0;
      for (int y = 0; y < recipe.getHeight(); y++) {
        StringBuilder row = new StringBuilder(recipe.getWidth());
        for (int x = 0; x < recipe.getWidth(); x++) {
          Optional<Ingredient> optionalIngredient = ingredients.get(y * recipe.getWidth() + x);
          if (optionalIngredient.isEmpty()) {
            row.append(' ');
          } else {
            Ingredient ingredient = optionalIngredient.get();
            Character symbol = symbols.get(ingredient);
            if (symbol == null) {
              if (nextSymbol >= SYMBOLS.length()) {
                throw new JsonSyntaxException("Too many distinct ingredients in shaped material recipe");
              }
              symbol = SYMBOLS.charAt(nextSymbol++);
              symbols.put(ingredient, symbol);
              key.add(symbol.toString(), Ingredient.CODEC.encodeStart(jsonOps, ingredient).getOrThrow());
            }
            row.append(symbol);
          }
        }
        pattern.add(row.toString());
      }
      json.add("key", key);
      json.add("pattern", pattern);
      json.add("result", ItemStack.CODEC.encodeStart(jsonOps, recipe.assemble(CraftingInput.EMPTY)).getOrThrow());
      if (!recipe.showNotification()) {
        json.addProperty("show_notification", false);
      }
      return json;
    }

  }

  private static ShapedMaterialsRecipe deserialize(JsonObject json, DynamicOps<?> ops) {
    return Serializer.fromJson(json, ops);
  }

  private static JsonObject serialize(ShapedMaterialsRecipe recipe, DynamicOps<?> ops) {
    JsonObject json = Serializer.serializeShapedBase(recipe, ops);
    StringBuilder partPattern = new StringBuilder(recipe.parts.size());
    List<Ingredient> inputs = recipe.pattern.ingredients().stream().flatMap(Optional::stream).toList();
    for (Ingredient part : recipe.parts) {
      int index = inputs.indexOf(part);
      if (index < 0) throw new JsonSyntaxException("Part ingredient is not present in shaped recipe inputs");
      int distinct = 0;
      java.util.IdentityHashMap<Ingredient,Character> symbols = new java.util.IdentityHashMap<>();
      for (Ingredient input : inputs) {
        if (!symbols.containsKey(input)) symbols.put(input, Serializer.SYMBOLS.charAt(distinct++));
      }
      partPattern.append(symbols.get(inputs.get(index)));
    }
    json.addProperty("parts", partPattern.toString());
    Serializer.MATERIAL_FIELD.serialize(recipe, json);
    return json;
  }

  private static void writeParts(RegistryFriendlyByteBuf buffer, ShapedMaterialsRecipe recipe) {
    Serializer.writeParts(buffer, recipe);
  }
}
