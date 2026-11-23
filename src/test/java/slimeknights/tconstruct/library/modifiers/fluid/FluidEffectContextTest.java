package slimeknights.tconstruct.library.modifiers.fluid;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FluidEffectContextTest {
  @Test void multipartTargetKeepsHitPartAndResolvesItsLivingParent() {
    var parent = mock(EnderDragon.class);
    var part = mock(EnderDragonPart.class);
    when(part.getParent()).thenReturn(parent);
    when(part.position()).thenReturn(Vec3.ZERO);
    var context = FluidEffectContext.builder(mock(Level.class)).target(part);
    assertThat(context.getTarget()).isSameAs(part);
    assertThat(context.getLivingTarget()).isSameAs(parent);
  }

  @SuppressWarnings("removal")
  @Test void legacyConstructorResolvesTheSameMultipartParent() {
    var parent = mock(EnderDragon.class);
    var part = mock(EnderDragonPart.class);
    when(part.getParent()).thenReturn(parent);
    when(part.position()).thenReturn(Vec3.ZERO);
    var context = new FluidEffectContext.Entity(mock(Level.class), null, null, part, Vec3.ZERO);
    assertThat(context.getLivingTarget()).isSameAs(parent);
  }

  @Test void ordinaryLivingAndNonLivingTargetsKeepTheirMeaning() {
    var living = mock(LivingEntity.class);
    when(living.position()).thenReturn(Vec3.ZERO);
    var builder = FluidEffectContext.builder(mock(Level.class));
    assertThat(builder.target(living).getLivingTarget()).isSameAs(living);
    var nonLiving = mock(Entity.class);
    when(nonLiving.position()).thenReturn(Vec3.ZERO);
    assertThat(builder.target(nonLiving).getLivingTarget()).isNull();
  }
}
