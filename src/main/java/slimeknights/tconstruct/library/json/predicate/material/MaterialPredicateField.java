package slimeknights.tconstruct.library.json.predicate.material;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;

import java.util.function.Function;

/** Field for an object-form material predicate. */
public record MaterialPredicateField<P>(String key, Function<P, IJsonPredicate<MaterialVariantId>> getter) implements LoadableField<IJsonPredicate<MaterialVariantId>,P> {

  @Override
  public IJsonPredicate<MaterialVariantId> get(JsonObject json, String key, TypedMap context) {
    if (json.has(key)) {
      return MaterialPredicate.LOADER.convert(json.get(key), key, context);
    }
    return MaterialPredicate.ANY;
  }

  @Override
  public void serialize(P parent, JsonObject json) {
    IJsonPredicate<MaterialVariantId> predicate = getter.apply(parent);
    if (predicate != MaterialPredicate.ANY) {
      JsonObject serialized = new JsonObject();
      MaterialPredicate.LOADER.serialize(predicate, serialized);
      json.add(key, serialized);
    }
  }

  @Override
  public IJsonPredicate<MaterialVariantId> decode(FriendlyByteBuf buffer, TypedMap context) {
    return MaterialPredicate.LOADER.decode(buffer, context);
  }

  @Override
  public void encode(FriendlyByteBuf buffer, P parent) {
    MaterialPredicate.LOADER.encode(buffer, getter.apply(parent));
  }
}
