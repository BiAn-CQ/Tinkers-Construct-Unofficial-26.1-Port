package slimeknights.tconstruct.client.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Strict resource-boundary representation of the native modifier-model format.
 * Every definition is an explicitly typed object and is decoded once before a
 * tool model is baked.
 */
final class NativeModifierModel {
  static final Codec<NativeModifierModel> CODEC = Codec.PASSTHROUGH.flatXmap(
    NativeModifierModel::decode,
    model -> DataResult.success(model.source)
  );

  private final Definition definition;
  private final Dynamic<?> source;

  private NativeModifierModel(Definition definition, Dynamic<?> source) {
    this.definition = definition;
    this.source = source;
  }

  Definition definition() {
    return definition;
  }

  /** Parses one entry from a modifier-model map resource. */
  static NativeModifierModel fromJson(JsonElement json) {
    return CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(JsonSyntaxException::new);
  }

  private static DataResult<NativeModifierModel> decode(Dynamic<?> source) {
    try {
      return DataResult.success(new NativeModifierModel(parse(source, "$"), source));
    } catch (IllegalArgumentException exception) {
      return DataResult.error(exception::getMessage);
    }
  }

  private static Definition parse(Dynamic<?> raw, String path) {
    String type = requiredString(raw, "type", path);
    return switch (type) {
      case "tconstruct:basic" -> texture(raw, path, TextureKind.BASIC);
      case "tconstruct:empty" -> Empty.INSTANCE;
      case "tconstruct:crafted" -> new Crafted(
        requiredIdentifier(raw, "modifier", path),
        parse(required(raw, "model", path), path + ".model")
      );
      case "tconstruct:trait" -> new Trait(
        requiredIdentifier(raw, "modifier", path),
        parse(required(raw, "model", path), path + ".model")
      );
      case "tconstruct:compound" -> {
        Dynamic<?> models = required(raw, "models", path);
        List<Dynamic<?>> values = listValues(models);
        if (values == null || values.isEmpty()) {
          throw error(path + ".models", "must be a non-empty array");
        }
        List<Definition> children = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
          children.add(parse(values.get(index), path + ".models[" + index + ']'));
        }
        yield new Compound(List.copyOf(children));
      }
      case "tconstruct:material_has_fallback" -> new MaterialFallback(
        nonNegativeInt(raw, "index", path),
        fallbackNames(raw, path),
        parse(required(raw, "if_true", path), path + ".if_true"),
        parse(required(raw, "if_false", path), path + ".if_false")
      );
      case "tconstruct:dyed" -> texture(raw, path, TextureKind.DYED);
      case "tconstruct:material" -> texture(raw, path, TextureKind.MATERIAL);
      case "tconstruct:persistent_material" -> new PersistentMaterial(
        requiredIdentifier(raw, "texture", path),
        optionalIdentifier(raw, "texture_large", path),
        optionalIdentifier(raw, "key", path)
      );
      case "tconstruct:potion" -> texture(raw, path, TextureKind.POTION);
      case "tconstruct:slimeskull" -> new Slimeskull(
        requiredIdentifier(raw, "texture", path),
        nonNegativeInt(raw, "skull_index", path),
        nonNegativeInt(raw, "slime_index", path)
      );
      case "tconstruct:fluid" -> fluid(raw, path);
      case "tconstruct:tank" -> tank(raw, path);
      case "tconstruct:banner" -> {
        Identifier prefix = optionalIdentifier(raw, "prefix", path);
        Identifier large = optionalIdentifier(raw, "prefix_large", path);
        if (prefix == null && large == null) {
          throw error(path, "banner modifier requires prefix or prefix_large");
        }
        yield new Banner(prefix, large);
      }
      case "tconstruct:armor_trim" -> {
        String slot = requiredString(raw, "slot", path);
        if (slot.isBlank()) {
          throw error(path + ".slot", "must not be blank");
        }
        yield new ArmorTrim(slot);
      }
      case "tconstruct:custom_trim" -> new CustomTrim(
        requiredIdentifier(raw, "root", path),
        optionalIdentifier(raw, "root_large", path)
      );
      default -> throw error(path + ".type", "unsupported modifier model type '" + type + "'");
    };
  }

  private static Texture texture(Dynamic<?> raw, String path, TextureKind kind) {
    return new Texture(
      kind,
      requiredIdentifier(raw, "texture", path),
      optionalIdentifier(raw, "texture_large", path),
      optionalInt(raw, "color", -1, path),
      lightLevel(raw, path)
    );
  }

  private static Fluid fluid(Dynamic<?> raw, String path) {
    Identifier texture = optionalIdentifier(raw, "texture", path);
    Identifier textureLarge = optionalIdentifier(raw, "texture_large", path);
    Identifier mask = optionalIdentifier(raw, "mask", path);
    Identifier maskLarge = optionalIdentifier(raw, "mask_large", path);
    if (texture == null && mask == null) {
      throw error(path, "fluid modifier requires texture or mask");
    }
    return new Fluid(texture, textureLarge, mask, maskLarge, tankHelper(raw, path));
  }

  private static Tank tank(Dynamic<?> raw, String path) {
    Identifier texture = optionalIdentifier(raw, "texture", path);
    Identifier textureLarge = optionalIdentifier(raw, "texture_large", path);
    Identifier mask = optionalIdentifier(raw, "mask", path);
    Identifier maskLarge = optionalIdentifier(raw, "mask_large", path);
    Identifier partial = optionalIdentifier(raw, "partial", path);
    Identifier partialLarge = optionalIdentifier(raw, "partial_large", path);
    Identifier full = optionalIdentifier(raw, "full", path);
    Identifier fullLarge = optionalIdentifier(raw, "full_large", path);
    if (texture == null && mask == null && partial == null && full == null) {
      throw error(path, "tank modifier requires texture, mask, partial, or full");
    }
    return new Tank(texture, textureLarge, mask, maskLarge, partial, partialLarge, full, fullLarge,
      nonNegativeOptionalInt(raw, "tolerance", 0, path), tankHelper(raw, path));
  }

  @Nullable
  private static Identifier tankHelper(Dynamic<?> raw, String path) {
    Identifier helper = optionalIdentifier(raw, "tank_helper", path);
    if (helper != null && !helper.toString().equals("tconstruct:smashing")) {
      throw error(path + ".tank_helper", "unsupported tank helper '" + helper + "'");
    }
    return helper;
  }

  private static Set<String> fallbackNames(Dynamic<?> raw, String path) {
    Dynamic<?> fallback = required(raw, "fallback", path);
    Optional<String> scalar = fallback.asString().result();
    if (scalar.isPresent()) {
      if (scalar.get().isBlank()) {
        throw error(path + ".fallback", "must not be blank");
      }
      return Set.of(scalar.get());
    }
    List<Dynamic<?>> values = listValues(fallback);
    if (values == null || values.isEmpty()) {
      throw error(path + ".fallback", "must be a string or non-empty string array");
    }
    Set<String> result = new HashSet<>();
    for (int index = 0; index < values.size(); index++) {
      String entryPath = path + ".fallback[" + index + ']';
      String value = values.get(index).asString().result()
        .orElseThrow(() -> error(entryPath, "must be a string"));
      if (value.isBlank()) {
        throw error(entryPath, "must not be blank");
      }
      result.add(value);
    }
    return Set.copyOf(result);
  }

  private static int lightLevel(Dynamic<?> raw, String path) {
    int value = optionalInt(raw, "luminosity", 0, path);
    if (value < 0 || value > 15) {
      throw error(path + ".luminosity", "must be between 0 and 15");
    }
    return value;
  }

  private static int nonNegativeInt(Dynamic<?> raw, String field, String path) {
    int value = integer(required(raw, field, path), path + '.' + field);
    if (value < 0) {
      throw error(path + '.' + field, "must not be negative");
    }
    return value;
  }

  private static int nonNegativeOptionalInt(Dynamic<?> raw, String field, int fallback, String path) {
    int value = optionalInt(raw, field, fallback, path);
    if (value < 0) {
      throw error(path + '.' + field, "must not be negative");
    }
    return value;
  }

  private static int optionalInt(Dynamic<?> raw, String field, int fallback, String path) {
    Optional<? extends Dynamic<?>> value = raw.get(field).result();
    if (value.isEmpty()) {
      return fallback;
    }
    return integer(value.get(), path + '.' + field);
  }

  private static int integer(Dynamic<?> raw, String path) {
    Number number = raw.asNumber().result().orElseThrow(() -> error(path, "must be an integer"));
    try {
      return new BigDecimal(number.toString()).intValueExact();
    } catch (ArithmeticException | NumberFormatException exception) {
      throw error(path, "must be a 32-bit integer");
    }
  }

  private static String requiredString(Dynamic<?> raw, String field, String path) {
    return required(raw, field, path).asString().result()
      .orElseThrow(() -> error(path + '.' + field, "must be a string"));
  }

  private static Identifier requiredIdentifier(Dynamic<?> raw, String field, String path) {
    return identifier(requiredString(raw, field, path), path + '.' + field);
  }

  @Nullable
  private static Identifier optionalIdentifier(Dynamic<?> raw, String field, String path) {
    Optional<? extends Dynamic<?>> value = raw.get(field).result();
    if (value.isEmpty()) {
      return null;
    }
    String text = value.get().asString().result()
      .orElseThrow(() -> error(path + '.' + field, "must be an identifier string"));
    return identifier(text, path + '.' + field);
  }

  private static Identifier identifier(String value, String path) {
    try {
      return Identifier.parse(value);
    } catch (RuntimeException exception) {
      throw error(path, "invalid identifier '" + value + "'");
    }
  }

  private static Dynamic<?> required(Dynamic<?> raw, String field, String path) {
    return raw.get(field).result().orElseThrow(() -> error(path + '.' + field, "is required"));
  }

  @Nullable
  private static List<Dynamic<?>> listValues(Dynamic<?> raw) {
    var stream = raw.asStreamOpt().result();
    if (stream.isEmpty()) {
      return null;
    }
    List<Dynamic<?>> values = new ArrayList<>();
    stream.get().forEach(values::add);
    return values;
  }

  private static IllegalArgumentException error(String path, String message) {
    return new IllegalArgumentException("Invalid modifier model at " + path + ": " + message);
  }

  sealed interface Definition permits Texture, Empty, Crafted, Trait, Compound, MaterialFallback,
    PersistentMaterial, Slimeskull, Fluid, Tank, Banner, ArmorTrim, CustomTrim {}

  enum TextureKind { BASIC, DYED, MATERIAL, POTION }

  record Texture(TextureKind kind, Identifier texture, @Nullable Identifier textureLarge, int color, int luminosity)
    implements Definition {}

  enum Empty implements Definition { INSTANCE }

  record Crafted(Identifier modifier, Definition model) implements Definition {}

  record Trait(Identifier modifier, Definition model) implements Definition {}

  record Compound(List<Definition> models) implements Definition {}

  record MaterialFallback(int index, Set<String> fallbacks, Definition ifTrue, Definition ifFalse)
    implements Definition {}

  record PersistentMaterial(Identifier texture, @Nullable Identifier textureLarge, @Nullable Identifier key)
    implements Definition {}

  record Slimeskull(Identifier texture, int skullIndex, int slimeIndex) implements Definition {}

  record Fluid(@Nullable Identifier texture, @Nullable Identifier textureLarge,
               @Nullable Identifier mask, @Nullable Identifier maskLarge,
               @Nullable Identifier tankHelper) implements Definition {}

  record Tank(@Nullable Identifier texture, @Nullable Identifier textureLarge,
              @Nullable Identifier mask, @Nullable Identifier maskLarge,
              @Nullable Identifier partial, @Nullable Identifier partialLarge,
              @Nullable Identifier full, @Nullable Identifier fullLarge,
              int tolerance, @Nullable Identifier tankHelper) implements Definition {}

  record Banner(@Nullable Identifier prefix, @Nullable Identifier prefixLarge) implements Definition {}

  record ArmorTrim(String slot) implements Definition {}

  record CustomTrim(Identifier root, @Nullable Identifier rootLarge) implements Definition {}
}
