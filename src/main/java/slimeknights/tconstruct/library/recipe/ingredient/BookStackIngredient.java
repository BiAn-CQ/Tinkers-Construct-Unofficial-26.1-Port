package slimeknights.tconstruct.library.recipe.ingredient;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.loadable.common.NBTLoadable;
import slimeknights.mantle.recipe.helper.LoadableIngredientSerializer;
import slimeknights.tconstruct.TConstruct;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Client-book ingredient that exposes one exact item stack with Tinkers custom data.
 *
 * <p>This exists because book icons need concrete display stacks, while the normal
 * components ingredient codec requires registry-aware JSON operations that the book
 * resource loader does not provide.</p>
 */
public final class BookStackIngredient implements ICustomIngredient {
  public static final Identifier ID = TConstruct.getResource("book_stack");
  public static final IngredientType<BookStackIngredient> TYPE = new IngredientType<>(
    LoadableIngredientSerializer.mapCodec(BookStackIngredient::parse, BookStackIngredient::serialize));

  private final Identifier itemId;
  private final JsonObject customDataJson;
  private final ItemStack stack;

  private BookStackIngredient(Identifier itemId, JsonObject customDataJson, ItemStack stack) {
    this.itemId = itemId;
    this.customDataJson = customDataJson;
    this.stack = stack;
  }

  private static BookStackIngredient parse(JsonObject json) {
    if (!json.has("item") || !json.get("item").isJsonPrimitive()) {
      throw new JsonParseException("Book stack ingredient requires a string 'item' field");
    }

    Identifier itemId = Identifier.tryParse(json.get("item").getAsString());
    if (itemId == null) {
      throw new JsonParseException("Invalid item ID in book stack ingredient: " + json.get("item"));
    }

    Item item = BuiltInRegistries.ITEM.getOptional(itemId)
      .orElseThrow(() -> new JsonParseException("Unknown item in book stack ingredient: " + itemId));

    JsonObject customDataJson = new JsonObject();
    if (json.has("custom_data")) {
      if (!json.get("custom_data").isJsonObject()) {
        throw new JsonParseException("Book stack ingredient 'custom_data' must be a JSON object");
      }
      customDataJson = json.getAsJsonObject("custom_data").deepCopy();
    }

    ItemStack stack = new ItemStack(item);
    if (customDataJson.size() > 0) {
      CompoundTag tag = NBTLoadable.DISALLOW_STRING.deserialize(customDataJson);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    return new BookStackIngredient(itemId, customDataJson, stack);
  }

  private static JsonObject serialize(BookStackIngredient ingredient) {
    JsonObject json = new JsonObject();
    json.addProperty("item", ingredient.itemId.toString());
    if (ingredient.customDataJson.size() > 0) {
      json.add("custom_data", ingredient.customDataJson.deepCopy());
    }
    return json;
  }

  @Override
  public boolean test(@Nullable ItemStack candidate) {
    return candidate != null && ItemStack.isSameItemSameComponents(candidate, stack);
  }

  @Override
  public Stream<Holder<Item>> items() {
    return Stream.of(BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem()));
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
    return this == object || object instanceof BookStackIngredient other
      && itemId.equals(other.itemId) && customDataJson.equals(other.customDataJson);
  }

  @Override
  public int hashCode() {
    return Objects.hash(itemId, customDataJson);
  }

}
