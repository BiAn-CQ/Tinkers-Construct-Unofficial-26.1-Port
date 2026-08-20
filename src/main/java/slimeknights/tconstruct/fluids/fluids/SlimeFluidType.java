package slimeknights.tconstruct.fluids.fluids;

import net.neoforged.neoforge.fluids.FluidType;
import slimeknights.mantle.fluid.InvertedFluidType;
import slimeknights.mantle.fluid.TextureFluidType;
import slimeknights.tconstruct.common.TinkerTags;

import net.minecraft.world.entity.LivingEntity;

/** Fluid type used by Tinkers slime fluids. Slimes do not drown in slime. */
public class SlimeFluidType extends TextureFluidType {
  public SlimeFluidType(Properties properties) {
    super(properties);
  }

  @Override
  public boolean canDrownIn(LivingEntity entity) {
    return !entity.getType().builtInRegistryHolder().is(TinkerTags.EntityTypes.SLIMES);
  }

  /** Inverted texture variant retaining the slime-specific drowning rule. */
  public static class Inverted extends InvertedFluidType {
    public Inverted(Properties properties) {
      super(properties);
    }

    @Override
    public boolean canDrownIn(LivingEntity entity) {
      return !entity.getType().builtInRegistryHolder().is(TinkerTags.EntityTypes.SLIMES);
    }
  }
}
