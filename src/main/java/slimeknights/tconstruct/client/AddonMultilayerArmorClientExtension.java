package slimeknights.tconstruct.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.utils.ItemStackDataUtil;

import javax.annotation.Nullable;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import slimeknights.mantle.data.loadable.common.ColorLoadable;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Native equipment-layer bridge for addon {@code MultilayerArmorItem}s.
 *
 * <p>The model remains owned by the resource pack under
 * {@code tinkering/armor_models}; this class only resolves the same material,
 * fixed, and persistent-data layers through the 26.1 equipment renderer.</p>
 */
final class AddonMultilayerArmorClientExtension implements TinkerArmorClientExtension {
  private static final List<AddonMultilayerArmorClientExtension> INSTANCES = new CopyOnWriteArrayList<>();
  private final Identifier modelName;
  @Nullable
  private ResourceManager cachedManager;
  private List<LayerDefinition> layers = List.of();

  AddonMultilayerArmorClientExtension(Identifier modelName) {
    this.modelName = modelName;
    INSTANCES.add(this);
  }

  static void invalidateAll() {
    INSTANCES.forEach(extension -> extension.cachedManager = null);
  }

  @Override
  @Nullable
  public Identifier getArmorTexture(ItemStack stack, EquipmentClientInfo.LayerType type,
                                    EquipmentClientInfo.Layer layer, Identifier defaultTexture) {
    if (!isSupported(type)) {
      return null;
    }
    LayerDefinition definition = find(layer.textureId());
    if (definition == null) {
      return null;
    }
    Selection selection = definition.select(stack, layer.textureId());
    return selection.texture() == null ? null : toTexturePath(selection.texture());
  }

  @Override
  public int getArmorLayerTintColor(ItemStack stack, EquipmentClientInfo.Layer layer,
                                    int layerIndex, int fallbackColor) {
    LayerDefinition definition = find(layer.textureId());
    if (definition == null) {
      return EquipmentLayerRenderer.getColorForLayer(layer, fallbackColor);
    }
    // -1 is opaque white for an already colored texture. The default dye color
    // is often 0, which NeoForge treats as "do not render this layer".
    Selection selection = definition.select(stack, layer.textureId());
    return selection.texture() == null ? 0 : selection.color();
  }

  @Override
  public int getArmorLuminosity(ItemStack stack, String layerPath) {
    LayerDefinition definition = findByPath(layerPath);
    if (definition == null) {
      return 0;
    }
    return definition.select(stack, Identifier.fromNamespaceAndPath(modelName.getNamespace(), layerPath)).luminosity();
  }

  private static boolean isSupported(EquipmentClientInfo.LayerType type) {
    return type == EquipmentClientInfo.LayerType.HUMANOID
      || type == EquipmentClientInfo.LayerType.HUMANOID_BABY
      || type == EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS
      || type == EquipmentClientInfo.LayerType.WINGS;
  }

  @Nullable
  private LayerDefinition find(Identifier layer) {
    refresh();
    for (LayerDefinition definition : layers) {
      if (definition.matches(layer)) {
        return definition;
      }
    }
    return null;
  }

  @Nullable
  private LayerDefinition findByPath(String path) {
    refresh();
    for (LayerDefinition definition : layers) {
      if (definition.matchesPath(path)) {
        return definition;
      }
    }
    return null;
  }

  private void refresh() {
    ResourceManager manager = Minecraft.getInstance().getResourceManager();
    if (manager == cachedManager) {
      return;
    }
    cachedManager = manager;
    Identifier resource = Identifier.fromNamespaceAndPath(
      modelName.getNamespace(), "tinkering/armor_models/" + modelName.getPath() + ".json");
    Optional<net.minecraft.server.packs.resources.Resource> found = manager.getResource(resource);
    if (found.isEmpty()) {
      TConstruct.LOG.warn("Missing addon armor model {}", resource);
      layers = List.of();
      return;
    }
    try (Reader reader = found.get().openAsReader()) {
      JsonArray entries = GsonHelper.getAsJsonArray(JsonParser.parseReader(reader).getAsJsonObject(), "layers");
      List<LayerDefinition> parsed = new ArrayList<>();
      for (JsonElement entry : entries) {
        if (!entry.isJsonObject()) {
          continue;
        }
        LayerDefinition definition = parseLayer(entry.getAsJsonObject());
        if (definition != null) {
          parsed.add(definition);
        }
      }
      layers = List.copyOf(parsed);
    } catch (RuntimeException | java.io.IOException exception) {
      TConstruct.LOG.error("Failed to load addon armor model {}", resource, exception);
      layers = List.of();
    }
  }

  @Nullable
  private static LayerDefinition parseLayer(JsonObject object) {
    String type = GsonHelper.getAsString(object, "type");
    if ("tconstruct:first_present".equals(type)) {
      List<LayerDefinition> options = new ArrayList<>();
      for (JsonElement option : GsonHelper.getAsJsonArray(object, "options")) {
        if (option.isJsonObject()) {
          LayerDefinition parsed = parseLayer(option.getAsJsonObject());
          if (parsed != null) {
            options.add(parsed);
          }
        }
      }
      return options.isEmpty() ? null : LayerDefinition.firstPresent(options);
    }
    if ("tconstruct:material_has_fallback".equals(type)) {
      LayerDefinition apply = parseLayer(GsonHelper.getAsJsonObject(object, "apply"));
      JsonElement fallback = object.get("fallback");
      Set<String> names = new java.util.HashSet<>();
      if (fallback.isJsonArray()) {
        fallback.getAsJsonArray().forEach(value -> names.add(value.getAsString()));
      } else {
        names.add(fallback.getAsString());
      }
      return apply == null ? null : new LayerDefinition(Kind.MATERIAL_FALLBACK, apply.prefix, apply.suffix,
        GsonHelper.getAsInt(object, "index"), null, -1, 0, List.of(apply), null, Set.copyOf(names));
    }
    // Unknown layer types need not have a prefix.
    if (!List.of("tconstruct:fixed", "tconstruct:material", "tconstruct:persistent_data", "tconstruct:dyed").contains(type)) {
      TConstruct.LOG.debug("Skipping unsupported addon armor layer type {}", type);
      return null;
    }
    Identifier prefix = Identifier.parse(GsonHelper.getAsString(object, "prefix"));
    String suffix = GsonHelper.getAsString(object, "suffix", "");
    return switch (type) {
      case "tconstruct:fixed" -> LayerDefinition.fixed(prefix, suffix,
        object.has("color") ? ColorLoadable.ALPHA.getIfPresent(object, "color") : -1,
        GsonHelper.getAsInt(object, "luminosity", 0),
        object.has("modifier") ? Identifier.parse(GsonHelper.getAsString(object, "modifier")) : null);
      case "tconstruct:material" -> LayerDefinition.material(prefix, GsonHelper.getAsInt(object, "index"));
      case "tconstruct:persistent_data" -> LayerDefinition.persistent(prefix,
        Identifier.parse(GsonHelper.getAsString(object, "material_key")));
      case "tconstruct:dyed" -> new LayerDefinition(Kind.DYED, prefix, suffix, -1,
        Identifier.parse(GsonHelper.getAsString(object, "modifier", "tconstruct:dyed")), -1,
        GsonHelper.getAsInt(object, "luminosity", 0), List.of(),
        object.has("default_color") ? ColorLoadable.NO_ALPHA.getIfPresent(object, "default_color") : null, Set.of());
      default -> null;
    };
  }

  private static Identifier toTexturePath(Identifier logical) {
    return logical.withPath(path -> "textures/tinker_armor/" + path + ".png");
  }

  private static boolean exists(Identifier logical) {
    return Minecraft.getInstance().getResourceManager().getResource(toTexturePath(logical)).isPresent();
  }

  private static Selection materialSelection(ItemStack stack, Identifier base, @Nullable MaterialVariantId material) {
    if (material == null || IMaterial.UNKNOWN_ID.equals(material)) {
      return new Selection(exists(base) ? base : null, -1, 0);
    }
    Optional<MaterialRenderInfo> renderInfo = MaterialRenderInfoLoader.INSTANCE.getRenderInfo(material);
    if (renderInfo.isPresent()) {
      MaterialRenderInfo info = renderInfo.get();
      Identifier texture = info.texture();
      if (texture != null) {
        Identifier candidate = base.withSuffix('_' + texture.getNamespace() + '_' + texture.getPath());
        if (exists(candidate)) {
          return new Selection(candidate, -1, info.luminosity());
        }
      }
      for (String fallback : info.fallbacks()) {
        Identifier candidate = base.withSuffix('_' + fallback);
        if (exists(candidate)) {
          return new Selection(candidate, info.vertexColor(), info.luminosity());
        }
      }
      return new Selection(exists(base) ? base : null, info.vertexColor(), info.luminosity());
    }
    return new Selection(exists(base) ? base : null, -1, 0);
  }

  private enum Kind { FIXED, MATERIAL, PERSISTENT, DYED, FIRST_PRESENT, MATERIAL_FALLBACK }

  private record LayerDefinition(Kind kind, Identifier prefix, String suffix, int index,
                                 @Nullable Identifier key, int color, int luminosity, List<LayerDefinition> options, @Nullable Integer defaultColor, Set<String> fallbacks) {
    static LayerDefinition fixed(Identifier prefix, String suffix, int color, int luminosity, @Nullable Identifier modifier) {
      return new LayerDefinition(Kind.FIXED, prefix, suffix, -1, modifier, color, luminosity, List.of(), null, Set.of());
    }

    static LayerDefinition material(Identifier prefix, int index) {
      return new LayerDefinition(Kind.MATERIAL, prefix, "", index, null, -1, 0, List.of(), null, Set.of());
    }

    static LayerDefinition persistent(Identifier prefix, Identifier key) {
      return new LayerDefinition(Kind.PERSISTENT, prefix, "", -1, key, -1, 0, List.of(), null, Set.of());
    }

    static LayerDefinition firstPresent(List<LayerDefinition> options) {
      LayerDefinition first = options.getFirst();
      return new LayerDefinition(Kind.FIRST_PRESENT, first.prefix, first.suffix, -1, null, -1, 0, List.copyOf(options), null, Set.of());
    }

    boolean matches(Identifier logical) {
      if ((kind == Kind.FIRST_PRESENT || kind == Kind.MATERIAL_FALLBACK)) {
        return options.stream().anyMatch(option -> option.matches(logical));
      }
      return logical.getNamespace().equals(prefix.getNamespace()) && matchesPath(logical.getPath());
    }

    boolean matchesPath(String path) {
      if ((kind == Kind.FIRST_PRESENT || kind == Kind.MATERIAL_FALLBACK)) {
        return options.stream().anyMatch(option -> option.matchesPath(path));
      }
      String prefixPath = prefix.getPath();
      return path.equals(prefixPath + "armor" + suffix)
        || path.equals(prefixPath + "leggings" + suffix)
        || path.equals(prefixPath + "wings" + suffix);
    }

    Selection select(ItemStack stack, Identifier base) {
      return switch (kind) {
        case FIXED -> key == null || ModifierUtil.getModifierLevel(stack, new ModifierId(key)) > 0
          ? new Selection(exists(base) ? base : null, color, luminosity) : new Selection(null, -1, 0);
        case MATERIAL -> materialSelection(stack, base, materialAt(stack, index));
        case PERSISTENT -> materialSelection(stack, base,
          MaterialVariantId.tryParse(ModifierUtil.getPersistentString(stack, key)));
        case DYED -> {
          var persistent = ToolStack.from(stack).getPersistentData();
          yield (defaultColor != null || ModifierUtil.getModifierLevel(stack, new ModifierId(key)) > 0) && exists(base)
            ? new Selection(base, 0xFF000000 | persistent.getIntOr(key, defaultColor == null ? -1 : defaultColor), luminosity)
            : new Selection(null, -1, 0);
        }
        case MATERIAL_FALLBACK -> {
          MaterialVariantId material = materialAt(stack, index);
          LayerDefinition apply = options.getFirst();
          yield material != null && MaterialRenderInfoLoader.INSTANCE.hasFallback(material, fallbacks)
            ? apply.select(stack, apply.baseFor(textureType(base))) : new Selection(null, -1, 0);
        }
        case FIRST_PRESENT -> {
          Selection selected = new Selection(null, -1, 0);
          for (LayerDefinition option : options) {
            selected = option.select(stack, option.baseFor(textureType(base)));
            if (selected.texture() != null) {
              break;
            }
          }
          yield selected;
        }
      };
    }

    private Identifier baseFor(String textureType) {
      return prefix.withSuffix(textureType + suffix);
    }

    private String textureType(Identifier base) {
      if (kind == Kind.FIRST_PRESENT || kind == Kind.MATERIAL_FALLBACK) {
        for (LayerDefinition option : options) {
          if (option.matches(base)) return option.textureType(base);
        }
      }
      for (String type : List.of("armor", "leggings", "wings")) {
        if (base.equals(baseFor(type))) return type;
      }
      throw new IllegalArgumentException("Unmatched armor layer " + base);
    }

    @Nullable
    private static MaterialVariantId materialAt(ItemStack stack, int index) {
      CompoundTag tag = ItemStackDataUtil.getTag(stack);
      if (tag == null || !tag.contains(ToolStack.TAG_MATERIALS)) {
        return null;
      }
      String value = tag.getListOrEmpty(ToolStack.TAG_MATERIALS).getStringOr(index, "");
      return value.isEmpty() ? null : MaterialVariantId.tryParse(value);
    }
  }

  private record Selection(@Nullable Identifier texture, int color, int luminosity) {}
}
