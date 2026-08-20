package slimeknights.tconstruct.tools.modules.cosmetic;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.loadable.record.SingletonLoader;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.hook.display.DisplayNameModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.utils.TinkerTooltipFlags;
import slimeknights.tconstruct.library.utils.Util;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/** Module for banner pattern tooltips */
public enum BannerModule implements ModifierModule, DisplayNameModifierHook, TooltipModifierHook {
  INSTANCE;

  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<BannerModule>defaultHooks(ModifierHooks.DISPLAY_NAME, ModifierHooks.TOOLTIP);
  public static final RecordLoadable<BannerModule> LOADER = new SingletonLoader<>(INSTANCE);
  /** Key for a dye color, stored as its ID */
  public static final String KEY_DYE = "dye";
  /** Key for a pattern color, as a 24 bit integer */
  public static final String KEY_COLOR = "color";
  /** Key for a pattern asset ID. Legacy short vanilla hashes are also accepted when reading. */
  public static final String KEY_PATTERN = "pattern";
  /** Tooltip key saying hold shift for patterns */
  private static final Component HOLD_SHIFT = TConstruct.makeTranslation("modifier", "banner.hold_shift").withStyle(ChatFormatting.GRAY);

  @Override
  public RecordLoadable<? extends ModifierModule> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  @Override
  public Component getDisplayName(IToolStackView tool, ModifierEntry entry, Component name, @Nullable RegistryAccess access) {
    // color the tooltip the color of the first pattern
    ListTag patterns = tool.getPersistentData().getList(patternKey(entry.getId()), ListTag.TAG_COMPOUND);
    if (!patterns.isEmpty()) {
      return name.copy().withStyle(name.getStyle().withColor(DyeColor.byId(patterns.getCompoundOrEmpty(0).getIntOr(KEY_DYE, 0)).getTextColor()));
    }
    return name;
  }

  @Override
  public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
    // add all patterns in a tinker station when holding
    if (tooltipFlag == TinkerTooltipFlags.TINKER_STATION) {
      if (tooltipKey == TooltipKey.SHIFT) {
        ListTag patterns = tool.getPersistentData().getList(patternKey(modifier.getId()), ListTag.TAG_COMPOUND);
        for (int i = 0; i < patterns.size(); i++) {
          CompoundTag tag = patterns.getCompoundOrEmpty(i);
          DyeColor dye = DyeColor.byId(tag.getIntOr(KEY_DYE, 0));
          Holder<BannerPattern> holder = findPattern(player.registryAccess(), tag.getStringOr(KEY_PATTERN, ""));
          if (holder != null) {
            // note that Forge is dumb in BannerItem with their patch - mojang already adds the mod ID to the tooltip key
            holder.unwrapKey().ifPresent(key ->
              tooltip.add(Component.translatable("block.minecraft.banner." + key.identifier().toShortLanguageKey() + '.' + dye.getName()).withStyle(ChatFormatting.GRAY)));

          }
        }
      } else {
        tooltip.add(HOLD_SHIFT);
      }
    }
  }

  /** Gets the key for the cache used in the model */
  public static Identifier cacheKey(ModifierId modifier) {
    return modifier.withSuffix("_cache");
  }

  /** Gets the key for the pattern list in NBT */
  public static Identifier patternKey(ModifierId modifier) {
    return modifier.withSuffix("_patterns");
  }

  /** Copies the given list of patterns from banner format to the tool's NBT */
  public static void copyPatterns(ModDataNBT data, ModifierId id, DyeColor dye, BannerPatternLayers banner) {
    int baseColor = Util.getColor(dye);
    ListTag patterns = new ListTag();

    // add in the base pattern, it only exists on shields and we copy from banners
    CompoundTag basePattern = new CompoundTag();
    basePattern.putString(KEY_PATTERN, BannerPatterns.BASE.identifier().toString());
    basePattern.putInt(KEY_DYE, dye.getId());
    basePattern.putInt(KEY_COLOR, baseColor);
    patterns.add(basePattern);

    // need a cache key, but it's just going to get hashed anyway, so store its hash
    int hashCode = baseColor;

    // add in all other patterns
    for (BannerPatternLayers.Layer layer : banner.layers()) {
      CompoundTag copy = new CompoundTag();
      String pattern = layer.pattern().value().assetId().toString();
      copy.putString(KEY_PATTERN, pattern);
      // convert the color from a dye color to an integer
      dye = layer.color();
      int color = Util.getColor(dye);
      copy.putInt(KEY_DYE, dye.getId()); // dye for the tooltip
      copy.putInt(KEY_COLOR, color); // color for the model
      // add the values
      patterns.add(copy);
      // update the hash code with the new information
      hashCode = 31 * (31 * hashCode + color) + pattern.hashCode();
    }

    // add to tool NBT
    data.put(patternKey(id), patterns);
    data.putInt(cacheKey(id), hashCode);
  }

  private static final Map<String,String> LEGACY_PATTERNS = Map.ofEntries(
    Map.entry("b", "base"), Map.entry("bl", "square_bottom_left"), Map.entry("br", "square_bottom_right"),
    Map.entry("tl", "square_top_left"), Map.entry("tr", "square_top_right"), Map.entry("bs", "stripe_bottom"),
    Map.entry("ts", "stripe_top"), Map.entry("ls", "stripe_left"), Map.entry("rs", "stripe_right"),
    Map.entry("cs", "stripe_center"), Map.entry("ms", "stripe_middle"), Map.entry("drs", "stripe_downright"),
    Map.entry("dls", "stripe_downleft"), Map.entry("ss", "small_stripes"), Map.entry("cr", "cross"),
    Map.entry("sc", "straight_cross"), Map.entry("bt", "triangle_bottom"), Map.entry("tt", "triangle_top"),
    Map.entry("bts", "triangles_bottom"), Map.entry("tts", "triangles_top"), Map.entry("ld", "diagonal_left"),
    Map.entry("rd", "diagonal_right"), Map.entry("lud", "diagonal_up_left"), Map.entry("rud", "diagonal_up_right"),
    Map.entry("mc", "circle"), Map.entry("mr", "rhombus"), Map.entry("vh", "half_vertical"),
    Map.entry("hh", "half_horizontal"), Map.entry("vhr", "half_vertical_right"), Map.entry("hhb", "half_horizontal_bottom"),
    Map.entry("bo", "border"), Map.entry("cbo", "curly_border"), Map.entry("gra", "gradient"),
    Map.entry("gru", "gradient_up"), Map.entry("bri", "bricks"), Map.entry("glb", "globe"),
    Map.entry("cre", "creeper"), Map.entry("sku", "skull"), Map.entry("flo", "flower"),
    Map.entry("moj", "mojang"), Map.entry("pig", "piglin")
  );

  @Nullable
  public static Identifier getAssetId(String storedId) {
    Identifier assetId = Identifier.tryParse(storedId);
    if (assetId == null || storedId.indexOf(':') < 0) {
      String path = LEGACY_PATTERNS.get(storedId);
      assetId = path == null ? null : Identifier.withDefaultNamespace(path);
    }
    return assetId;
  }

  @Nullable
  public static Holder<BannerPattern> findPattern(HolderLookup.Provider access, String storedId) {
    Identifier assetId = getAssetId(storedId);
    if (assetId == null) {
      return null;
    }
    Identifier target = assetId;
    return access.lookupOrThrow(Registries.BANNER_PATTERN).listElements()
      .filter(holder -> holder.value().assetId().equals(target)).findFirst().orElse(null);
  }
}
