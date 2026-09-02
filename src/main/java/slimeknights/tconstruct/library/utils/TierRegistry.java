package slimeknights.tconstruct.library.utils;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Stable IDs and ordering for vanilla mining tiers after NeoForge removed TierSortingRegistry. */
public final class TierRegistry {
  private static final List<ToolMaterial> SORTED_TIERS = List.of(
    ToolMaterial.WOOD, ToolMaterial.STONE, ToolMaterial.IRON,
    ToolMaterial.GOLD, ToolMaterial.DIAMOND, ToolMaterial.NETHERITE);
  private static final Map<ToolMaterial,Identifier> NAMES = Map.of(
    ToolMaterial.WOOD, Identifier.withDefaultNamespace("wood"),
    ToolMaterial.STONE, Identifier.withDefaultNamespace("stone"),
    ToolMaterial.IRON, Identifier.withDefaultNamespace("iron"),
    ToolMaterial.GOLD, Identifier.withDefaultNamespace("gold"),
    ToolMaterial.DIAMOND, Identifier.withDefaultNamespace("diamond"),
    ToolMaterial.NETHERITE, Identifier.withDefaultNamespace("netherite"));

  private TierRegistry() {}

  public static Identifier getName(ToolMaterial tier) {
    return Objects.requireNonNull(NAMES.get(tier), "Unknown tool material");
  }

  public static ToolMaterial byName(Identifier id) {
    return NAMES.entrySet().stream().filter(entry -> entry.getValue().equals(id)).map(Map.Entry::getKey).findFirst().orElse(ToolMaterial.WOOD);
  }

  public static List<ToolMaterial> getSortedTiers() {
    return SORTED_TIERS;
  }

  public static boolean isCorrectTierForDrops(ToolMaterial tier, BlockState state) {
    return !state.is(tier.incorrectBlocksForDrops());
  }
}
