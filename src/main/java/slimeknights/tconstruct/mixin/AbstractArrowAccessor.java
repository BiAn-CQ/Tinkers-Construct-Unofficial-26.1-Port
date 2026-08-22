package slimeknights.tconstruct.mixin;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;

/** Exposes the vanilla piercing hit set so modifier hooks obey the same pierce limit as the arrow. */
@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {
  @Accessor("piercingIgnoreEntityIds")
  @Nullable IntOpenHashSet tconstruct$getPiercingIgnoreEntityIds();

  /** Updates vanilla's synced pierce level for any arrow implementation. */
  @Invoker("setPierceLevel")
  void tconstruct$setPierceLevel(byte pierceLevel);
}
