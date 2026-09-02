package slimeknights.tconstruct.library.utils;

import com.google.common.collect.Maps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import slimeknights.mantle.client.ResourceColorManager;
import slimeknights.mantle.data.listener.ISafeManagerReloadListener;
import slimeknights.tconstruct.TConstruct;

import java.util.List;
import java.util.Map;

/**
 * Harvest level display names
 */
public class HarvestTiers {
  private HarvestTiers() {}

  /** Cache of name for each tier */
  private static final Map<ToolMaterial, Component> harvestLevelNames = Maps.newHashMap();
  /** Listener to clear name cache so we get new colors */
  public static final ISafeManagerReloadListener RELOAD_LISTENER = manager -> harvestLevelNames.clear();

  /** Makes a translation key for the given name */
  private static MutableComponent makeLevelKey(ToolMaterial tier) {
    String key = Util.makeTranslationKey("harvest_tier", TierRegistry.getName(tier));
    TextColor color = ResourceColorManager.getTextColor(key);
    return TConstruct.makeTranslation("stat", key).withStyle(style -> style.withColor(color));
  }

  /**
   * Gets the harvest level name for the given level number
   * @param tier  ToolMaterial
   * @return  Level name
   */
  public static Component getName(ToolMaterial tier) {
    return harvestLevelNames.computeIfAbsent(tier, n ->  makeLevelKey(tier));
  }

  /** Gets the larger of two tiers */
  public static ToolMaterial max(ToolMaterial a, ToolMaterial b) {
    List<ToolMaterial> sorted = TierRegistry.getSortedTiers();
    // note indexOf returns -1 if the tier is missing, so the larger of an unsorted tier and a sorted one is the sorted one
    if (sorted.indexOf(b) > sorted.indexOf(a)) {
      return b;
    }
    return a;
  }

  /** Gets the smaller of two tiers */
  public static ToolMaterial min(ToolMaterial a, ToolMaterial b) {
    List<ToolMaterial> sorted = TierRegistry.getSortedTiers();
    // note indexOf returns -1 if the tier is missing, so the smaller of an unsorted tier and a sorted one is the unsorted one
    if (sorted.indexOf(b) < sorted.indexOf(a)) {
      return b;
    }
    return a;
  }

  /** Gets the smallest tier in the sorting registry */
  public static ToolMaterial minTier() {
    List<ToolMaterial> sortedTiers = TierRegistry.getSortedTiers();
    if (sortedTiers.isEmpty()) {
      TConstruct.LOG.error("No sorted tiers exist, this should not happen");
      return ToolMaterial.WOOD;
    }
    return sortedTiers.get(0);
  }

  /** Gets the block tag populated for the given vanilla tool material. */
  public static TagKey<Block> getRequiredTag(ToolMaterial material) {
    if (material.equals(ToolMaterial.WOOD)) {
      return Tags.Blocks.NEEDS_WOOD_TOOL;
    }
    if (material.equals(ToolMaterial.GOLD)) {
      return Tags.Blocks.NEEDS_GOLD_TOOL;
    }
    if (material.equals(ToolMaterial.STONE)) {
      return BlockTags.NEEDS_STONE_TOOL;
    }
    if (material.equals(ToolMaterial.IRON)) {
      return BlockTags.NEEDS_IRON_TOOL;
    }
    if (material.equals(ToolMaterial.DIAMOND)) {
      return BlockTags.NEEDS_DIAMOND_TOOL;
    }
    if (material.equals(ToolMaterial.NETHERITE)) {
      return Tags.Blocks.NEEDS_NETHERITE_TOOL;
    }
    throw new IllegalArgumentException("Unknown tool material");
  }
}
