package slimeknights.tconstruct.library.client.materials;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import slimeknights.mantle.client.ResourceColorManager;
import slimeknights.mantle.data.listener.ISafeManagerReloadListener;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.utils.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/** Client cache for material translation keys, names, and colors. */
public final class MaterialTooltipCache {
  /** Map of the key for each material variant */
  private static final Map<MaterialVariantId,String> KEY_CACHE = new HashMap<>();
  /** Map of the color for each material variant */
  private static final Map<MaterialVariantId,TextColor> COLOR_CACHE = new HashMap<>();
  /** Map of the display name for each material variant */
  private static final Map<MaterialVariantId,Component> DISPLAY_NAME_CACHE = new HashMap<>();
  /** Map of the colored display name for each material variant */
  private static final Map<MaterialVariantId,Component> COLORED_DISPLAY_NAME_CACHE = new HashMap<>();

  /** Clears all resource-pack-driven caches. */
  private static final ISafeManagerReloadListener RELOAD_LISTENER = manager -> {
    KEY_CACHE.clear();
    COLOR_CACHE.clear();
    DISPLAY_NAME_CACHE.clear();
    COLORED_DISPLAY_NAME_CACHE.clear();
  };

  private MaterialTooltipCache() {}

  /** Registers cache invalidation with client resource reloads. */
  public static void init(AddClientReloadListenersEvent event) {
    event.addListener(TConstruct.getResource("material_tooltip"), RELOAD_LISTENER);
  }

  /** Converts a material ID into its translation key. */
  private static final Function<MaterialVariantId,String> KEY_GETTER = id ->
    Util.makeTranslationKey("material", id.getLocation('.'));

  /** Gets the translation key for a material variant. */
  public static String getKey(MaterialVariantId variantId) {
    return KEY_CACHE.computeIfAbsent(variantId, KEY_GETTER);
  }

  /** Gets the translated display name for a material variant. */
  private static final Function<MaterialVariantId,MutableComponent> DISPLAY_NAME_GETTER = id -> {
    if (id.hasVariant()) {
      String variantKey = getKey(id);
      if (Util.canTranslate(variantKey)) {
        return Component.translatable(variantKey);
      }
    }
    return Component.translatable(getKey(id.getId()));
  };

  /** Gets the translated display name for a material variant. */
  public static Component getDisplayName(MaterialVariantId variantId) {
    return DISPLAY_NAME_CACHE.computeIfAbsent(variantId, DISPLAY_NAME_GETTER);
  }

  /** Gets the configured text color for a material variant. */
  private static final Function<MaterialVariantId,TextColor> COLOR_GETTER = id -> {
    if (id.hasVariant()) {
      String variantKey = getKey(id);
      TextColor color = ResourceColorManager.getOrNull(variantKey);
      if (color != null) {
        return color;
      }
    }
    return ResourceColorManager.getTextColor(getKey(id.getId()));
  };

  /** Gets the configured text color for a material variant. */
  public static TextColor getColor(MaterialVariantId id) {
    return COLOR_CACHE.computeIfAbsent(id, COLOR_GETTER);
  }

  /** Gets the translated display name with the configured material color. */
  private static final Function<MaterialVariantId,Component> COLORED_DISPLAY_NAME_GETTER = id -> {
    TextColor color = getColor(id);
    if ((color.getValue() & 0xFFFFFF) != 0xFFFFFF) {
      return DISPLAY_NAME_GETTER.apply(id).withStyle(style -> style.withColor(color));
    }
    return getDisplayName(id);
  };

  /** Gets the colored translated display name for a material variant. */
  public static Component getColoredDisplayName(MaterialVariantId variantId) {
    return COLORED_DISPLAY_NAME_CACHE.computeIfAbsent(variantId, COLORED_DISPLAY_NAME_GETTER);
  }
}
