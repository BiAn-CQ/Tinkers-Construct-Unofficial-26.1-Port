package slimeknights.tconstruct.fluids.item;

import net.minecraft.world.level.material.Fluid;
import java.util.function.Supplier;

/** @deprecated use the shared potion bucket. */
@Deprecated(forRemoval = true)
public class PotionBucketItem extends slimeknights.mantle.fluid.PotionBucketItem {
  static final int DRINK_DURATION = 96;
  static final float POTION_DURATION_SCALE = 2.5F;
  public PotionBucketItem(Supplier<? extends Fluid> supplier, Properties properties) { super(supplier, properties); }
}
