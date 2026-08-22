package slimeknights.tconstruct.mixin;

import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Keeps Tinkers' modifier-backed hit state aligned with the private vanilla trident lifecycle state. */
@Mixin(ThrownTrident.class)
public interface ThrownTridentAccessor {
  @Accessor("dealtDamage")
  void tconstruct$setDealtDamage(boolean dealtDamage);
}
