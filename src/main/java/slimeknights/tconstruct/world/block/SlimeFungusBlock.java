package slimeknights.tconstruct.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.NetherFungusBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.world.TinkerWorld;

/** Update of fungus that grows on slime soil instead */
public class SlimeFungusBlock extends NetherFungusBlock {
  public SlimeFungusBlock(Properties properties, ResourceKey<ConfiguredFeature<?,?>> fungusFeature) {
    super(fungusFeature, TinkerWorld.slimeDirt.get(DirtType.ICHOR), TinkerTags.Blocks.SLIMY_SOIL, properties);
  }

  @Override
  public boolean isValidBonemealTarget(LevelReader worldIn, BlockPos pos, BlockState state) {
    return worldIn.getBlockState(pos.below()).is(TinkerTags.Blocks.SLIMY_SOIL);
  }
}
