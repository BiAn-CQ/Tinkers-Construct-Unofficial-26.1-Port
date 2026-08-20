package slimeknights.tconstruct.plugin.jsonthings.block;

import dev.gigaherz.jsonthings.things.blocks.FlexLiquidBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FlowingFluid;

import java.util.Map;
import java.util.function.Supplier;

/** Json Things version of {@link slimeknights.tconstruct.fluids.block.BurningLiquidBlock} */
public class FlexBurningLiquidBlock extends FlexLiquidBlock {
  private final int burnTime;
  private final float damage;
  public FlexBurningLiquidBlock(Properties properties, Map<Property<?>,Comparable<?>> propertyDefaultValues, Supplier<FlowingFluid> fluidSupplier, int burnTime, float damage) {
    super(properties, propertyDefaultValues, fluidSupplier);
    this.burnTime = burnTime;
    this.damage = damage;
  }

  @Override
  protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
    if (!entity.fireImmune()) {
      entity.igniteForSeconds(burnTime);
      if (entity.hurtOrSimulate(level.damageSources().lava(), damage)) {
        entity.playSound(SoundEvents.GENERIC_BURN, 0.4F, 2.0F + level.getRandom().nextFloat() * 0.4F);
      }
    }
  }
}
