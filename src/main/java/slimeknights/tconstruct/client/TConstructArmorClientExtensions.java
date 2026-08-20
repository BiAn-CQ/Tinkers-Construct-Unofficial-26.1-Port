package slimeknights.tconstruct.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.utils.ItemStackDataUtil;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.TinkerTools;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Bridges Tinkers' layered armor textures to the native 26.1 equipment renderer.
 *
 * <p>The old renderer supplied a new texture for every layer at render time. In
 * 26.1 the vanilla layer list is still the right geometry path, but NeoForge
 * exposes the final texture choice through {@link IClientItemExtensions}. This
 * class keeps that choice data-driven without reviving the pre-26.1 model API.</p>
 */
final class TConstructArmorClientExtensions implements IClientItemExtensions {
  private static final Identifier TEXTURE_ROOT = Identifier.fromNamespaceAndPath("tconstruct", "textures/tinker_armor");
  private static final ModifierId DYED = TinkerModifiers.dyed.getId();
  /** Slime wings have one material part; keep all equipment-layer lookups on that part. */
  private static final int SLIME_WINGS_MATERIAL_INDEX = 0;

  private static final TConstructArmorClientExtensions TRAVELERS = new TConstructArmorClientExtensions(Family.TRAVELERS);
  private static final TConstructArmorClientExtensions PLATE = new TConstructArmorClientExtensions(Family.PLATE);
  private static final TConstructArmorClientExtensions SLIME = new TConstructArmorClientExtensions(Family.SLIME);
  private static final TConstructArmorClientExtensions SLIME_WINGS = new TConstructArmorClientExtensions(Family.SLIME_WINGS);

  private final Family family;

  private TConstructArmorClientExtensions(Family family) {
    this.family = family;
  }

  /** Registers all Tinkers armor items once the client extension registry is available. */
  static void register(RegisterClientExtensionsEvent event) {
    event.registerItem(TRAVELERS, TinkerTools.travelersGear.values().toArray(Item[]::new));
    event.registerItem(PLATE, TinkerTools.plateArmor.values().toArray(Item[]::new));
    event.registerItem(SLIME, TinkerTools.slimesuit.values().toArray(Item[]::new));
    event.registerItem(SLIME_WINGS, TinkerTools.slimeWings.get());
  }

  @Override
  @Nullable
  public Identifier getArmorTexture(ItemStack stack, EquipmentClientInfo.LayerType type,
                                    EquipmentClientInfo.Layer layer, Identifier defaultTexture) {
    if (type != EquipmentClientInfo.LayerType.HUMANOID
        && type != EquipmentClientInfo.LayerType.HUMANOID_BABY
        && type != EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS
        && type != EquipmentClientInfo.LayerType.WINGS) {
      return null;
    }

    String layerPath = layer.textureId().getPath();
    boolean leggings = type == EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS;
    Identifier logical = switch (family) {
      case TRAVELERS -> travelersTexture(stack, layerPath, leggings);
      case PLATE -> plateTexture(stack, layerPath, leggings);
      case SLIME -> slimeTexture(stack, layerPath, leggings);
      case SLIME_WINGS -> slimeWingsTexture(stack, layerPath);
    };
    return logical == null ? null : toTexturePath(logical);
  }

  @Override
  public int getArmorLayerTintColor(ItemStack stack, EquipmentClientInfo.Layer layer, int layerIdx, int fallbackColor) {
    String layerPath = layer.textureId().getPath();
    return switch (family) {
      case TRAVELERS -> travelersColor(stack, layerPath, layerIdx, fallbackColor);
      case PLATE -> plateColor(stack, layerPath, layerIdx, fallbackColor);
      case SLIME -> slimeColor(stack, layerPath, layerIdx, fallbackColor);
      case SLIME_WINGS -> slimeWingsColor(stack, layerPath, layerIdx, fallbackColor);
    };
  }

  /**
   * Returns the material light level for a native equipment layer.
   *
   * <p>NeoForge's 26.1 armor extension exposes texture and tint overrides but
   * no light-level override.  The compatibility glow layer uses this value to
   * add the same full-bright pass that {@code TintedArmorTexture} used before
   * the equipment renderer migration.</p>
   */
  int getArmorLuminosity(ItemStack stack, String layerPath) {
    return switch (family) {
      case TRAVELERS -> {
        if (layerPath.endsWith("/cuirass_armor") || layerPath.endsWith("/cuirass_leggings")) {
          yield materialLuminosity(stack, 1);
        }
        if (layerPath.endsWith("/metal_armor") || layerPath.endsWith("/metal_leggings")) {
          yield materialLuminosity(stack, 0);
        }
        yield 0;
      }
      case PLATE -> {
        if (layerPath.endsWith("/plating_armor") || layerPath.endsWith("/plating_leggings")) {
          yield materialLuminosity(stack, 0);
        }
        if (layerPath.endsWith("/maille_armor") || layerPath.endsWith("/maille_leggings")) {
          yield materialLuminosity(stack, 1);
        }
        yield 0;
      }
      case SLIME -> layerPath.endsWith("/armor") || layerPath.endsWith("/leggings")
        ? materialLuminosity(stack, 1) : 0;
      case SLIME_WINGS -> layerPath.endsWith("/wings") ? materialLuminosity(stack, SLIME_WINGS_MATERIAL_INDEX) : 0;
    };
  }

  @Nullable
  private Identifier travelersTexture(ItemStack stack, String layerPath, boolean leggings) {
    if (layerPath.endsWith("/base_leggings")) {
      return null;
    }
    if (layerPath.endsWith("/base_armor")) {
      return logical("travelers/base_armor");
    }
    if (layerPath.endsWith("/cuirass_leggings") || layerPath.endsWith("/cuirass_armor")) {
      Identifier prefix = logical("travelers/cuirass_" + (leggings ? "leggings" : "armor"));
      if (hasDyed(stack)) {
        return prefix;
      }
      return materialTexture(stack, 1, prefix).texture();
    }
    if (layerPath.endsWith("/metal_leggings") || layerPath.endsWith("/metal_armor")) {
      Identifier prefix = logical("travelers/metal_" + (leggings ? "leggings" : "armor"));
      return materialTexture(stack, 0, prefix).texture();
    }
    return null;
  }

  private int travelersColor(ItemStack stack, String layerPath, int layerIdx, int fallbackColor) {
    if (layerPath.endsWith("/base_leggings")) {
      return 0;
    }
    if (layerPath.endsWith("/base_armor")) {
      return -1;
    }
    if (layerPath.endsWith("/cuirass_leggings") || layerPath.endsWith("/cuirass_armor")) {
      if (hasDyed(stack)) {
        return dyeColor(stack);
      }
      return materialTexture(stack, 1, logical("travelers/cuirass_" + armorPart(layerPath))).color();
    }
    if (layerPath.endsWith("/metal_leggings") || layerPath.endsWith("/metal_armor")) {
      return materialTexture(stack, 0, logical("travelers/metal_" + armorPart(layerPath))).color();
    }
    return fallbackColor;
  }

  @Nullable
  private Identifier plateTexture(ItemStack stack, String layerPath, boolean leggings) {
    if (layerPath.endsWith("/plating_leggings") || layerPath.endsWith("/plating_armor")) {
      return materialTexture(stack, 0, logical("plate/plating_" + (leggings ? "leggings" : "armor"))).texture();
    }
    if (layerPath.endsWith("/maille_leggings") || layerPath.endsWith("/maille_armor")) {
      Identifier prefix = logical("plate/maille_" + (leggings ? "leggings" : "armor"));
      MaterialVariantId material = getMaterial(stack, 1);
      if (hasDyed(stack) && hasMetalFallback(material)) {
        Identifier metal = prefix.withSuffix("_metal");
        return exists(metal) ? metal : logical("plate/maille_" + (leggings ? "leggings_cloth" : "armor_cloth"));
      }
      if (hasDyed(stack)) {
        return logical("plate/maille_" + (leggings ? "leggings_cloth" : "armor_cloth"));
      }
      return materialTexture(stack, 1, prefix).texture();
    }
    return null;
  }

  private int plateColor(ItemStack stack, String layerPath, int layerIdx, int fallbackColor) {
    if (layerPath.endsWith("/plating_leggings") || layerPath.endsWith("/plating_armor")) {
      return materialTexture(stack, 0, logical("plate/plating_" + armorPart(layerPath))).color();
    }
    if (layerPath.endsWith("/maille_leggings") || layerPath.endsWith("/maille_armor")) {
      MaterialVariantId material = getMaterial(stack, 1);
      if (hasDyed(stack)) {
        if (hasMetalFallback(material)) {
          return dyeColor(stack);
        }
        return dyeColor(stack);
      }
      return materialTexture(stack, 1, logical("plate/maille_" + armorPart(layerPath))).color();
    }
    return fallbackColor;
  }

  @Nullable
  private Identifier slimeTexture(ItemStack stack, String layerPath, boolean leggings) {
    if (layerPath.endsWith("/armor") || layerPath.endsWith("/leggings")) {
      return materialTexture(stack, 1, logical("slime/" + (leggings ? "leggings" : "armor"))).texture();
    }
    return null;
  }

  private int slimeColor(ItemStack stack, String layerPath, int layerIdx, int fallbackColor) {
    if (layerPath.endsWith("/armor") || layerPath.endsWith("/leggings")) {
      return materialTexture(stack, 1, logical("slime/" + (layerPath.endsWith("/leggings") ? "leggings" : "armor"))).color();
    }
    return fallbackColor;
  }

  @Nullable
  private Identifier slimeWingsTexture(ItemStack stack, String layerPath) {
    if (layerPath.endsWith("/wings")) {
      return materialTexture(stack, SLIME_WINGS_MATERIAL_INDEX, logical("slime/wings")).texture();
    }
    return null;
  }

  private int slimeWingsColor(ItemStack stack, String layerPath, int layerIdx, int fallbackColor) {
    if (layerPath.endsWith("/wings")) {
      return materialTexture(stack, SLIME_WINGS_MATERIAL_INDEX, logical("slime/wings")).color();
    }
    return fallbackColor;
  }

  private boolean hasDyed(ItemStack stack) {
    return ModifierUtil.getModifierLevel(stack, DYED) > 0;
  }

  private int dyeColor(ItemStack stack) {
    return ModifierUtil.getPersistentInt(stack, DYED.location(), -1);
  }

  private boolean hasMetalFallback(@Nullable MaterialVariantId material) {
    if (material == null) {
      return false;
    }
    Optional<MaterialRenderInfo> info = MaterialRenderInfoLoader.INSTANCE.getRenderInfo(material);
    return info.isPresent() && (contains(info.get().fallbacks(), "metal") || contains(info.get().fallbacks(), "metal_contrast"));
  }

  private static boolean contains(String[] values, String target) {
    for (String value : values) {
      if (target.equals(value)) {
        return true;
      }
    }
    return false;
  }

  private MaterialSelection materialTexture(ItemStack stack, int index, Identifier prefix) {
    MaterialVariantId material = getMaterial(stack, index);
    if (material == null || IMaterial.UNKNOWN_ID.equals(material)) {
      return MaterialSelection.EMPTY;
    }

    Optional<MaterialRenderInfo> infoOptional = MaterialRenderInfoLoader.INSTANCE.getRenderInfo(material);
    if (infoOptional.isPresent()) {
      MaterialRenderInfo info = infoOptional.get();
      if (info.texture() != null) {
        Identifier candidate = prefix.withSuffix("_" + info.texture().getNamespace() + "_" + info.texture().getPath());
        if (exists(candidate)) {
          return new MaterialSelection(candidate, -1);
        }
      }
      for (String fallback : info.fallbacks()) {
        Identifier candidate = prefix.withSuffix("_" + fallback);
        if (exists(candidate)) {
          return new MaterialSelection(candidate, info.vertexColor());
        }
      }
      return exists(prefix) ? new MaterialSelection(prefix, info.vertexColor()) : MaterialSelection.EMPTY;
    }
    return exists(prefix) ? new MaterialSelection(prefix, -1) : MaterialSelection.EMPTY;
  }

  private int materialLuminosity(ItemStack stack, int index) {
    MaterialVariantId material = getMaterial(stack, index);
    if (material == null || IMaterial.UNKNOWN_ID.equals(material)) {
      return 0;
    }
    return MaterialRenderInfoLoader.INSTANCE.getRenderInfo(material)
      .map(MaterialRenderInfo::luminosity)
      .orElse(0);
  }

  @Nullable
  private static MaterialVariantId getMaterial(ItemStack stack, int index) {
    CompoundTag tag = ItemStackDataUtil.getTag(stack);
    if (tag == null || !tag.contains(ToolStack.TAG_MATERIALS)) {
      return null;
    }
    String value = tag.getListOrEmpty(ToolStack.TAG_MATERIALS).getStringOr(index, "");
    return value.isEmpty() ? null : MaterialVariantId.tryParse(value);
  }

  private static Identifier logical(String path) {
    return Identifier.fromNamespaceAndPath("tconstruct", path);
  }

  private static String armorPart(String layerPath) {
    return layerPath.endsWith("_leggings") ? "leggings" : "armor";
  }

  private static Identifier toTexturePath(Identifier logical) {
    return logical.withPath(path -> TEXTURE_ROOT.getPath() + "/" + path + ".png");
  }

  private static boolean exists(Identifier logical) {
    return Minecraft.getInstance().getResourceManager().getResource(toTexturePath(logical)).isPresent();
  }

  private record MaterialSelection(@Nullable Identifier texture, int color) {
    private static final MaterialSelection EMPTY = new MaterialSelection(null, 0);
  }

  private enum Family {
    TRAVELERS,
    PLATE,
    SLIME,
    SLIME_WINGS
  }
}
