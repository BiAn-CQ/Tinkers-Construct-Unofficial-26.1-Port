package slimeknights.tconstruct.library.recipe.material;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import slimeknights.mantle.data.JsonCodec;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Small adapter for the 26.1 immutable recipe serializer record. */
final class TinkerCraftingRecipeSerializer {
  private TinkerCraftingRecipeSerializer() {}

  static <T extends Recipe<?>> RecipeSerializer<T> create(
    Function<JsonObject, T> decoder,
    Function<T, JsonObject> encoder,
    BiConsumer<RegistryFriendlyByteBuf, T> streamEncoder,
    Function<RegistryFriendlyByteBuf, T> streamDecoder) {
    return create((json, ignoredOps) -> decoder.apply(json), encoder, streamEncoder, streamDecoder);
  }

  static <T extends Recipe<?>> RecipeSerializer<T> create(
    BiFunction<JsonObject, DynamicOps<?>, T> decoder,
    Function<T, JsonObject> encoder,
    BiConsumer<RegistryFriendlyByteBuf, T> streamEncoder,
    Function<RegistryFriendlyByteBuf, T> streamDecoder) {
    return create(decoder, (value, ignoredOps) -> encoder.apply(value), streamEncoder, streamDecoder);
  }

  static <T extends Recipe<?>> RecipeSerializer<T> create(
    BiFunction<JsonObject, DynamicOps<?>, T> decoder,
    BiFunction<T, DynamicOps<?>, JsonObject> encoder,
    BiConsumer<RegistryFriendlyByteBuf, T> streamEncoder,
    Function<RegistryFriendlyByteBuf, T> streamDecoder) {
    JsonCodec<T> codec = new JsonCodec<>() {
      @Override
      public T deserialize(JsonElement element, DynamicOps<?> ops) {
        if (!element.isJsonObject()) {
          throw new JsonParseException("Expected recipe to be a JSON object");
        }
        return decoder.apply(element.getAsJsonObject(), ops);
      }

      @Override
      public JsonElement serialize(T value, DynamicOps<?> ops) {
        return encoder.apply(value, ops);
      }
    };
    MapCodec<T> mapCodec = MapCodec.assumeMapUnsafe(codec);
    StreamCodec<RegistryFriendlyByteBuf, T> streamCodec = StreamCodec.of(
      (buffer, value) -> streamEncoder.accept(buffer, value), streamDecoder::apply);
    return new RecipeSerializer<>(mapCodec, streamCodec);
  }

  /**
   * Returns JSON operations retaining registry lookups when the recipe
   * manager supplied registry-aware operations.  The native 26.1 codecs use
   * this to resolve item holders and dynamic tags.
   */
  static DynamicOps<JsonElement> registryJsonOps(DynamicOps<?> ops) {
    if (ops instanceof net.minecraft.resources.RegistryOps<?> registryOps) {
      return net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, registryOps.lookupProvider);
    }
    return com.mojang.serialization.JsonOps.INSTANCE;
  }
}
