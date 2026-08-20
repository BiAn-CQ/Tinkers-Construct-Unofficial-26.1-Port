package slimeknights.tconstruct.world.worldgen.trees;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import slimeknights.tconstruct.world.TinkerStructures;
import slimeknights.tconstruct.world.block.FoliageType;

import java.util.Optional;

public final class SlimeTree {
  private SlimeTree() {}

  /** Creates the native 1.21 tree grower matching the former AbstractTreeGrower selection logic. */
  public static TreeGrower create(FoliageType foliageType) {
    ResourceKey<ConfiguredFeature<?, ?>> tree = switch (foliageType) {
      case EARTH -> TinkerStructures.earthSlimeTree;
      case SKY -> TinkerStructures.skySlimeTree;
      case ENDER -> TinkerStructures.enderSlimeTree;
      case BLOOD -> TinkerStructures.bloodSlimeFungus;
      case ICHOR -> TinkerStructures.ichorSlimeFungus;
    };
    if (foliageType == FoliageType.ENDER) {
      return new TreeGrower("tconstruct_ender_slime", 0.85f, Optional.empty(), Optional.empty(), Optional.of(tree),
                            Optional.of(TinkerStructures.enderSlimeTreeTall), Optional.empty(), Optional.empty());
    }
    return new TreeGrower("tconstruct_" + foliageType.getSerializedName() + "_slime", Optional.empty(), Optional.of(tree), Optional.empty());
  }
}
