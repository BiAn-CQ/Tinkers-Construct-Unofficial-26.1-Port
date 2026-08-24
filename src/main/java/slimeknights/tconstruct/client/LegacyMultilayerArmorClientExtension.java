package slimeknights.tconstruct.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
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
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Native equipment-layer bridge for addon {@code MultilayerArmorItem}s.
 *
 * <p>The model remains owned by the resource pack under
 * {@code tinkering/armor_models}; this class only resolves the same material,
 * fixed, and persistent-data layers through the 26.1 equipment renderer.</p>
 */
final class LegacyMultilayerArmorClientExtension implements TinkerArmorClientExtension {
  private static final List<LegacyMultilayerArmorClientExtension> INSTANCES = new CopyOnWriteArrayList<>();
  private final Identifier modelName;
  @Nullable
  private ResourceManager cachedManager;
  private List<LayerDefinition> layers = List.of();

  LegacyMultilayerArmorClientExtension(Identifier modelName) {
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
      return fallbackColor;
    }
    int color = definition.select(stack, layer.textureId()).color();
    return color == -1 ? fallbackColor : color;
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
        JsonObject object = entry.getAsJsonObject();
        String type = GsonHelper.getAsString(object, "type");
        Identifier prefix = Identifier.parse(GsonHelper.getAsString(object, "prefix"));
        String suffix = GsonHelper.getAsString(object, "suffix", "");
        switch (type) {
          case "tconstruct:fixed" -> parsed.add(LayerDefinition.fixed(prefix, suffix,
            GsonHelper.getAsInt(object, "color", -1), GsonHelper.getAsInt(object, "luminosity", 0)));
          case "tconstruct:material" -> parsed.add(LayerDefinition.material(prefix,
            GsonHelper.getAsInt(object, "index")));
          case "tconstruct:persistent_data" -> parsed.add(LayerDefinition.persistent(prefix,
            Identifier.parse(GsonHelper.getAsString(object, "material_key"))));
          default -> TConstruct.LOG.debug("Skipping unsupported addon armor layer type {} in {}", type, resource);
        }
      }
      layers = List.copyOf(parsed);
    } catch (RuntimeException | java.io.IOException exception) {
      TConstruct.LOG.error("Failed to load addon armor model {}", resource, exception);
      layers = List.of();
    }
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

  private enum Kind { FIXED, MATERIAL, PERSISTENT }

  private record LayerDefinition(Kind kind, Identifier prefix, String suffix, int index,
                                 @Nullable Identifier key, int color, int luminosity) {
    static LayerDefinition fixed(Identifier prefix, String suffix, int color, int luminosity) {
      return new LayerDefinition(Kind.FIXED, prefix, suffix, -1, null, color, luminosity);
    }

    static LayerDefinition material(Identifier prefix, int index) {
      return new LayerDefinition(Kind.MATERIAL, prefix, "", index, null, -1, 0);
    }

    static LayerDefinition persistent(Identifier prefix, Identifier key) {
      return new LayerDefinition(Kind.PERSISTENT, prefix, "", -1, key, -1, 0);
    }

    boolean matches(Identifier logical) {
      return logical.getNamespace().equals(prefix.getNamespace()) && matchesPath(logical.getPath());
    }

    boolean matchesPath(String path) {
      String prefixPath = prefix.getPath();
      return path.equals(prefixPath + "armor" + suffix)
        || path.equals(prefixPath + "leggings" + suffix)
        || path.equals(prefixPath + "wings" + suffix);
    }

    Selection select(ItemStack stack, Identifier base) {
      return switch (kind) {
        case FIXED -> new Selection(exists(base) ? base : null, color, luminosity);
        case MATERIAL -> materialSelection(stack, base, materialAt(stack, index));
        case PERSISTENT -> materialSelection(stack, base,
          MaterialVariantId.tryParse(ModifierUtil.getPersistentString(stack, key)));
      };
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
