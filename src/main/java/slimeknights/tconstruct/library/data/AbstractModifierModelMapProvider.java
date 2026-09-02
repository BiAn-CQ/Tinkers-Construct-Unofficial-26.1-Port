package slimeknights.tconstruct.library.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import slimeknights.mantle.data.GenericDataProvider;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.registration.object.IdAwareObject;
import slimeknights.tconstruct.library.tools.definition.ArmorSlotType;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.data.ModifierIds;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Data provider for modifier model maps.
 *
 * <p>The generated resources use explicit typed objects so the native item
 * model codec has a single unambiguous input shape.</p>
 */
public abstract class AbstractModifierModelMapProvider extends GenericDataProvider {
  /** Argument for {@code largeSeparator} to disable large textures entirely. */
  protected static final char SMALL = '\0';

  private final Map<Identifier,Builder> models = new LinkedHashMap<>();
  private final String modId;

  protected AbstractModifierModelMapProvider(PackOutput output, String modId) {
    super(output, Target.RESOURCE_PACK, "tinkering/modifier_models");
    this.modId = modId;
  }

  /** Adds all model maps. */
  protected abstract void addModels();

  @Override
  public CompletableFuture<?> run(CachedOutput output) {
    addModels();
    return allOf(models.entrySet().stream()
      .filter(entry -> !entry.getValue().isEmpty())
      .map(entry -> saveJson(output, entry.getKey(), entry.getValue().build())));
  }

  /** Creates an item-atlas texture identifier. */
  protected Identifier material(String texture) {
    return Identifier.fromNamespaceAndPath(modId, texture);
  }

  /** Creates a tool texture identifier. */
  protected Identifier toolMaterial(String texture) {
    return material("item/tool/" + texture);
  }

  protected Builder tool(Identifier tool, Identifier base) {
    return models.computeIfAbsent(tool, id -> new Builder(base));
  }

  protected Builder tool(Identifier tool) {
    return tool(tool, tool);
  }

  protected Builder tool(String tool) {
    return tool(Identifier.fromNamespaceAndPath(modId, tool));
  }

  protected Builder tool(Identifier tool, String variant) {
    return tool(tool.withSuffix(variant), tool);
  }

  protected Builder tool(IdAwareObject tool) {
    return tool(tool.getId());
  }

  protected Builder tool(IdAwareObject tool, String variant) {
    return tool(tool.getId(), variant);
  }

  protected Builder tool(Item tool) {
    return tool(Loadables.ITEM.getKey(tool));
  }

  protected Builder tool(Item tool, String variant) {
    return tool(Loadables.ITEM.getKey(tool), variant);
  }

  /** Converts a modifier identifier into its texture suffix. */
  protected static String suffix(ModifierId modifier) {
    return modifier.getNamespace() + '_' + modifier.getPath();
  }

  private static JsonObject typed(String type) {
    JsonObject json = new JsonObject();
    json.addProperty("type", "tconstruct:" + type);
    return json;
  }

  private static void addTexture(JsonObject json, String key, @Nullable Identifier texture) {
    if (texture != null) {
      json.addProperty(key, texture.toString());
    }
  }

  private static JsonElement basicModel(@Nullable Identifier small, @Nullable Identifier large, int luminosity) {
    return basicModelObject(small, large, luminosity);
  }

  /** Creates the object form of a basic model, used inside compound model arrays. */
  private static JsonObject basicModelObject(@Nullable Identifier small, @Nullable Identifier large, int luminosity) {
    JsonObject json = typed("basic");
    addTexture(json, "texture", small);
    addTexture(json, "texture_large", large);
    if (luminosity != 0) {
      json.addProperty("luminosity", luminosity);
    }
    return json;
  }

  private static JsonObject simpleModel(String type, @Nullable Identifier small, @Nullable Identifier large) {
    JsonObject json = typed(type);
    addTexture(json, "texture", small);
    addTexture(json, "texture_large", large);
    return json;
  }

  /** Builder for one tool or tool variant. */
  protected final class Builder {
    private final Map<String,JsonElement> constant = new LinkedHashMap<>();
    private final Map<ModifierId,JsonElement> modifiers = new LinkedHashMap<>();
    private final Identifier id;

    private Builder(Identifier id) {
      this.id = id;
    }

    private JsonElement merge(JsonElement model, JsonElement... additional) {
      if (additional.length == 0) {
        return model;
      }
      JsonArray array = new JsonArray();
      array.add(model);
      for (JsonElement child : additional) {
        array.add(child);
      }
      JsonObject compound = typed("compound");
      compound.add("models", array);
      return compound;
    }

    public Builder constant(String name, JsonElement model, JsonElement... additional) {
      JsonElement previous = constant.putIfAbsent(name, merge(model, additional));
      if (previous != null) {
        throw new IllegalArgumentException("Duplicate constant model: " + name);
      }
      return this;
    }

    public Builder modifier(ModifierId modifier, JsonElement model, JsonElement... additional) {
      JsonElement previous = modifiers.putIfAbsent(modifier, merge(model, additional));
      if (previous != null) {
        throw new IllegalArgumentException("Duplicate modifier model: " + modifier);
      }
      return this;
    }

    /** Overrides a lower-priority modifier map with an empty model. */
    public Builder empty(ModifierId modifier) {
      return modifier(modifier, typed("empty"));
    }

    /** Overrides lower-priority modifier maps with empty models. */
    public Builder empty(ModifierId... modifiers) {
      for (ModifierId modifier : modifiers) {
        empty(modifier);
      }
      return this;
    }

    @Nullable
    private String largeFolder(String folder, char separator) {
      return separator == SMALL ? null : folder + "/large" + separator + "modifiers";
    }

    public Builder luminosity(int light, ModifierId modifier, String texture, @Nullable String largeTexture) {
      return modifier(modifier, basicModel(toolMaterial(texture), largeTexture == null ? null : toolMaterial(largeTexture), light));
    }

    public Builder luminosity(int light, String folder, @Nullable String largeFolder, ModifierId... modifierIds) {
      for (ModifierId modifier : modifierIds) {
        String textureSuffix = '/' + suffix(modifier);
        luminosity(light, modifier, folder + textureSuffix, largeFolder == null ? null : largeFolder + textureSuffix);
      }
      return this;
    }

    public Builder luminosity(int light, String folder, @Nullable String largeFolder, String textureSuffix, ModifierId... modifierIds) {
      for (ModifierId modifier : modifierIds) {
        String suffix = '/' + suffix(modifier) + textureSuffix;
        luminosity(light, modifier, folder + suffix, largeFolder == null ? null : largeFolder + suffix);
      }
      return this;
    }

    public Builder luminosity(int light, char largeSeparator, ModifierId... modifierIds) {
      String path = id.getPath();
      return luminosity(light, path + "/modifiers", largeFolder(path, largeSeparator), modifierIds);
    }

    public Builder luminosity(int light, char largeSeparator, String textureSuffix, ModifierId... modifierIds) {
      String path = id.getPath();
      return luminosity(light, path + "/modifiers", largeFolder(path, largeSeparator), textureSuffix, modifierIds);
    }

    public Builder basic(ModifierId modifier, String texture, @Nullable String largeTexture) {
      return luminosity(0, modifier, texture, largeTexture);
    }

    public Builder basic(String folder, @Nullable String largeFolder, ModifierId... modifierIds) {
      return luminosity(0, folder, largeFolder, modifierIds);
    }

    public Builder basic(char largeSeparator, ModifierId... modifierIds) {
      return luminosity(0, largeSeparator, modifierIds);
    }

    public Builder basic(char largeSeparator, String textureSuffix, ModifierId... modifierIds) {
      return luminosity(0, largeSeparator, textureSuffix, modifierIds);
    }

    public Builder compact(String folder, ModifierId... modifierIds) {
      for (ModifierId modifier : modifierIds) {
        basic(modifier, folder + '/' + modifier.getPath(), null);
      }
      return this;
    }

    public Builder compact(ModifierId... modifierIds) {
      return compact(id.getPath() + "/modifiers", modifierIds);
    }

    /* Common constants */

    /** Adds a nested model that is shown when the given trait modifier is present. */
    public Builder trait(String key, ModifierId modifier, JsonElement model, JsonElement... additional) {
      JsonObject nested = typed("trait");
      nested.addProperty("modifier", modifier.toString());
      nested.add("model", merge(model, additional));
      return constant(key, nested);
    }

    /** Adds a nested model that is shown when the given trait modifier is present. */
    public Builder trait(ModifierId modifier, JsonElement model, JsonElement... additional) {
      return trait(modifier.getPath(), modifier, model, additional);
    }

    /** Adds a nested model that is shown in the crafted modifier layer. */
    public Builder first(String key, ModifierId modifier, JsonElement model, JsonElement... additional) {
      JsonObject nested = typed("crafted");
      nested.addProperty("modifier", modifier.toString());
      nested.add("model", merge(model, additional));
      return constant(key, nested);
    }

    /** Adds a nested model near the material layers using the standard modifier key. */
    public Builder first(ModifierId modifier, JsonElement model, JsonElement... additional) {
      return first('_' + modifier.getPath(), modifier, model, additional);
    }

    public Builder fluid(String folder) {
      JsonObject tank = typed("tank");
      addTexture(tank, "partial", toolMaterial(folder + "/fluid_partial"));
      addTexture(tank, "full", toolMaterial(folder + "/fluid_full"));
      return constant("fluid", tank);
    }

    public Builder fluid() {
      return fluid(id.getPath());
    }

    public Builder fluid(String folder, @Nullable String largeFolder, ModifierId modifier) {
      String name = suffix(modifier);
      JsonObject fluid = typed("fluid");
      addTexture(fluid, "mask", toolMaterial(folder + '/' + name + "_full"));
      if (largeFolder != null) {
        addTexture(fluid, "mask_large", toolMaterial(largeFolder + '/' + name + "_full"));
      }
      JsonElement overlay = basicModelObject(toolMaterial(folder + '/' + name),
        largeFolder == null ? null : toolMaterial(largeFolder + '/' + name), 0);
      return modifier(modifier, fluid, overlay);
    }

    public Builder fluid(ModifierId modifier, char largeSeparator) {
      String path = id.getPath();
      return fluid(path + "/modifiers", largeFolder(path, largeSeparator), modifier);
    }

    public Builder tank(String folder, @Nullable String largeFolder) {
      String name = suffix(ModifierIds.tank);
      JsonObject tank = typed("tank");
      addTexture(tank, "partial", toolMaterial(folder + '/' + name + "_partial"));
      addTexture(tank, "full", toolMaterial(folder + '/' + name + "_full"));
      if (largeFolder != null) {
        addTexture(tank, "partial_large", toolMaterial(largeFolder + '/' + name + "_partial"));
        addTexture(tank, "full_large", toolMaterial(largeFolder + '/' + name + "_full"));
      }
      JsonElement overlay = basicModelObject(toolMaterial(folder + '/' + name),
        largeFolder == null ? null : toolMaterial(largeFolder + '/' + name), 0);
      return modifier(ModifierIds.tank, tank, overlay);
    }

    public Builder tank(char largeSeparator) {
      String path = id.getPath();
      return tank(path + "/modifiers", largeFolder(path, largeSeparator));
    }

    public Builder smashing(String texture) {
      JsonObject fluid = typed("fluid");
      addTexture(fluid, "mask", toolMaterial(texture));
      fluid.addProperty("tank_helper", "tconstruct:smashing");
      return trait(ModifierIds.smashing, fluid);
    }

    public Builder tipped(String texture) {
      return trait("__tipped", ModifierIds.tipped, simpleModel("potion", toolMaterial(texture), null));
    }

    /** Adds a crafted dyed model before normal modifier textures. */
    public Builder dyed(JsonElement model, JsonElement... additional) {
      return first("__dyed", TinkerModifiers.dyed.getId(), model, additional);
    }

    /** Adds a crafted dyed model before normal modifier textures. */
    public Builder dyed(String smallTexture, @Nullable String largeTexture) {
      return dyed(simpleModel("dyed", toolMaterial(smallTexture),
        largeTexture == null ? null : toolMaterial(largeTexture)));
    }

    /** Adds a crafted dyed model for a small texture. */
    public Builder dyed(String smallTexture) {
      return dyed(smallTexture, null);
    }

    public Builder trim(ArmorSlotType type) {
      JsonObject trim = typed("armor_trim");
      trim.addProperty("slot", type.getName());
      return first(TinkerModifiers.trim.getId(), trim);
    }

    public Builder customTrim(String folder, @Nullable String largeTexture) {
      JsonObject trim = typed("custom_trim");
      addTexture(trim, "root", toolMaterial(folder + "/trim"));
      if (largeTexture != null) {
        addTexture(trim, "root_large", toolMaterial(folder + '/' + largeTexture));
      }
      return first(TinkerModifiers.trim.getId(), trim);
    }

    public Builder customTrim(@Nullable String largeTexture) {
      return customTrim(id.getPath(), largeTexture);
    }

    public Builder embellishment(String folder, @Nullable String largeFolder) {
      ModifierId embellishment = TinkerModifiers.embellishment.getId();
      String name = '/' + suffix(embellishment);
      return first("__embellishment", embellishment, simpleModel("persistent_material", toolMaterial(folder + name),
        largeFolder == null ? null : toolMaterial(largeFolder + name)))
        .empty(embellishment);
    }

    public Builder embellishment(char largeSeparator) {
      String path = id.getPath();
      return embellishment(path + "/modifiers", largeFolder(path, largeSeparator));
    }

    public Builder banner(@Nullable String smallPrefix, @Nullable String largePrefix) {
      JsonObject banner = typed("banner");
      addTexture(banner, "prefix", smallPrefix == null ? null : toolMaterial(smallPrefix));
      addTexture(banner, "prefix_large", largePrefix == null ? null : toolMaterial(largePrefix));
      return first(TinkerModifiers.banner.getId(), banner);
    }

    public Builder materialFallbackDyed(int index, String ifTrueTexture, String ifFalseTexture, String... fallbacks) {
      JsonObject conditional = typed("material_has_fallback");
      conditional.addProperty("index", index);
      if (fallbacks.length == 1) {
        conditional.addProperty("fallback", fallbacks[0]);
      } else {
        JsonArray array = new JsonArray();
        for (String fallback : fallbacks) {
          array.add(fallback);
        }
        conditional.add("fallback", array);
      }
      conditional.add("if_true", simpleModel("dyed", toolMaterial(ifTrueTexture), null));
      conditional.add("if_false", simpleModel("dyed", toolMaterial(ifFalseTexture), null));
      return dyed(conditional);
    }

    /** Adds the slime helmet skull model, which derives its texture from two material slots. */
    public Builder slimeskull(String texture, int skullIndex, int slimeIndex) {
      JsonObject skull = simpleModel("slimeskull", toolMaterial(texture), null);
      skull.addProperty("skull_index", skullIndex);
      skull.addProperty("slime_index", slimeIndex);
      return constant("__skull", skull);
    }

    public Builder emptyConstant(String name) {
      return constant(name, typed("empty"));
    }

    private boolean isEmpty() {
      return constant.isEmpty() && modifiers.isEmpty();
    }

    private JsonObject build() {
      JsonObject json = new JsonObject();
      if (!constant.isEmpty()) {
        JsonObject values = new JsonObject();
        constant.forEach(values::add);
        json.add("constant", values);
      }
      if (!modifiers.isEmpty()) {
        JsonObject values = new JsonObject();
        modifiers.forEach((modifier, model) -> values.add(modifier.toString(), model));
        json.add("modifiers", values);
      }
      return json;
    }
  }
}
