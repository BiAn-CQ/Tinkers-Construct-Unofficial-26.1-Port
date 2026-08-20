package slimeknights.tconstruct.library.utils;

import net.minecraft.resources.Identifier;
import slimeknights.tconstruct.library.compat.Tier;
import slimeknights.tconstruct.library.compat.Tiers;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.List;

/** Stable IDs and ordering for vanilla mining tiers after NeoForge removed TierSortingRegistry. */
public final class TierRegistry {
  private TierRegistry() {}

  public static Identifier getName(Tier tier) {
    return Identifier.withDefaultNamespace(tier.toString().toLowerCase(java.util.Locale.ROOT));
  }

  public static Tier byName(Identifier id) {
    return Arrays.stream(Tiers.values()).filter(tier -> getName(tier).equals(id)).findFirst().orElse(Tiers.WOOD);
  }

  public static List<Tier> getSortedTiers() {
    return List.of(Tiers.WOOD, Tiers.STONE, Tiers.IRON, Tiers.GOLD, Tiers.DIAMOND, Tiers.NETHERITE);
  }

  public static boolean isCorrectTierForDrops(Tier tier, BlockState state) {
    return !state.is(tier.getIncorrectBlocksForDrops());
  }
}
