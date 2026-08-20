package slimeknights.tconstruct.world.worldgen.trees.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.HugeFungusConfiguration;
import net.minecraft.world.level.levelgen.feature.HugeFungusFeature;
import net.minecraft.world.level.levelgen.feature.WeepingVinesFeature;
import slimeknights.tconstruct.world.worldgen.trees.config.SlimeFungusConfig;

public class SlimeFungusFeature extends HugeFungusFeature {
  public SlimeFungusFeature(Codec<HugeFungusConfiguration> codec) {
    super(codec);
  }

  @Override
  public boolean place(FeaturePlaceContext<HugeFungusConfiguration> context) {
    if (!(context.config() instanceof SlimeFungusConfig config)) {
      return super.place(context);
    }
    // must be on the right ground
    WorldGenLevel level = context.level();
    BlockPos pos = context.origin();
    if (!level.getBlockState(pos.below()).is(config.getGroundTag())) {
      return false;
    }
    // ensure not too tall
    RandomSource random = context.random();
    int height = Mth.nextInt(random, 4, 13);
    if (random.nextInt(12) == 0) {
      height *= 2;
    }
    if (!config.planted && pos.getY() + height + 1 >= context.chunkGenerator().getGenDepth()) {
      return false;
    }
    // actual generation
    boolean flag = !config.planted && random.nextFloat() < 0.06F;
    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 4);
    this.placeStem(level, random, config, pos, height, flag);
    this.placeHat(level, random, config, pos, height, flag);
    return true;
  }

  private static boolean isReplaceable(WorldGenLevel level, BlockPos pos, HugeFungusConfiguration config, boolean checkConfig) {
    return level.isStateAtPosition(pos, BlockBehaviour.BlockStateBase::canBeReplaced)
      || checkConfig && config.replaceableBlocks.test(level, pos);
  }

  private void placeStem(WorldGenLevel level, RandomSource random, HugeFungusConfiguration config, BlockPos pos, int height, boolean huge) {
    BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
    BlockState stem = config.stemState;
    int radius = huge ? 1 : 0;
    for (int x = -radius; x <= radius; x++) {
      for (int z = -radius; z <= radius; z++) {
        boolean corner = huge && Mth.abs(x) == radius && Mth.abs(z) == radius;
        for (int y = 0; y < height; y++) {
          mutable.setWithOffset(pos, x, y, z);
          if (isReplaceable(level, mutable, config, true)) {
            if (config.planted) {
              if (!level.getBlockState(mutable.below()).isAir()) {
                level.destroyBlock(mutable, true);
              }
              level.setBlock(mutable, stem, 3);
            } else if (!corner || random.nextFloat() < 0.1F) {
              setBlock(level, mutable, stem);
            }
          }
        }
      }
    }
  }

  private void placeHat(WorldGenLevel level, RandomSource random, HugeFungusConfiguration config, BlockPos pos, int height, boolean huge) {
    BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
    boolean weepingVines = config.hatState.is(Blocks.NETHER_WART_BLOCK);
    int hatHeight = Math.min(random.nextInt(1 + height / 3) + 5, height);
    int bottom = height - hatHeight;
    for (int y = bottom; y <= height; y++) {
      int radius = y < height - random.nextInt(3) ? 2 : 1;
      if (hatHeight > 8 && y < bottom + 4) {
        radius = 3;
      }
      if (huge) {
        radius++;
      }
      for (int x = -radius; x <= radius; x++) {
        for (int z = -radius; z <= radius; z++) {
          boolean xEdge = x == -radius || x == radius;
          boolean zEdge = z == -radius || z == radius;
          boolean inside = !xEdge && !zEdge && y != height;
          boolean corner = xEdge && zEdge;
          boolean lowerHat = y < bottom + 3;
          mutable.setWithOffset(pos, x, y, z);
          if (isReplaceable(level, mutable, config, false)) {
            if (config.planted && !level.getBlockState(mutable.below()).isAir()) {
              level.destroyBlock(mutable, true);
            }
            if (lowerHat) {
              if (!inside) {
                placeHatDropBlock(level, random, mutable, config.hatState, weepingVines);
              }
            } else if (inside) {
              placeHatBlock(level, random, config, mutable, 0.1F, 0.2F, weepingVines ? 0.1F : 0.0F);
            } else if (corner) {
              placeHatBlock(level, random, config, mutable, 0.01F, 0.7F, weepingVines ? 0.083F : 0.0F);
            } else {
              placeHatBlock(level, random, config, mutable, 5.0E-4F, 0.98F, weepingVines ? 0.07F : 0.0F);
            }
          }
        }
      }
    }
  }

  private void placeHatBlock(LevelAccessor level, RandomSource random, HugeFungusConfiguration config, BlockPos.MutableBlockPos pos,
                             float decorationChance, float hatChance, float vineChance) {
    if (random.nextFloat() < decorationChance) {
      setBlock(level, pos, config.decorState);
    } else if (random.nextFloat() < hatChance) {
      setBlock(level, pos, config.hatState);
      if (random.nextFloat() < vineChance) {
        tryPlaceWeepingVines(pos, level, random);
      }
    }
  }

  private void placeHatDropBlock(LevelAccessor level, RandomSource random, BlockPos pos, BlockState state, boolean weepingVines) {
    if (level.getBlockState(pos.below()).is(state.getBlock())) {
      setBlock(level, pos, state);
    } else if (random.nextFloat() < 0.15F) {
      setBlock(level, pos, state);
      if (weepingVines && random.nextInt(11) == 0) {
        tryPlaceWeepingVines(pos, level, random);
      }
    }
  }

  private static void tryPlaceWeepingVines(BlockPos pos, LevelAccessor level, RandomSource random) {
    BlockPos.MutableBlockPos mutable = pos.mutable().move(Direction.DOWN);
    if (level.isEmptyBlock(mutable)) {
      int length = Mth.nextInt(random, 1, 5);
      if (random.nextInt(7) == 0) {
        length *= 2;
      }
      WeepingVinesFeature.placeWeepingVinesColumn(level, random, mutable, length, 23, 25);
    }
  }
}
