package slimeknights.tconstruct.tools.modules.interaction.sling;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SlingKnockbackModuleTest {
  private Entity target(double z) {
    Entity entity = mock(Entity.class);
    when(entity.getBoundingBox()).thenReturn(new AABB(-0.3, 0, z - 0.3, 0.3, 1.8, z + 0.3));
    return entity;
  }

  @Test
  void hitsWhenEyesAreInsideExpandedTargetBox() {
    Entity close = target(0.5);
    Vec3 eye = new Vec3(0, 1.6, 0);
    assertThat(close.getBoundingBox().inflate(1).clip(eye, eye.add(0, 0, 5))).isEmpty();
    var hit = SlingKnockbackModule.findTarget(List.of(close), eye, eye.add(0, 0, 5));
    assertThat(hit).isNotNull();
    assertThat(hit.getEntity()).isSameAs(close);
    assertThat(hit.getLocation().z()).isCloseTo(0.2, org.assertj.core.data.Offset.offset(0.00001));
  }

  @Test
  void selectsNearestTargetAndMissesTargetsOutsideRay() {
    Entity close = target(0.5);
    Entity far = target(4);
    Vec3 eye = new Vec3(0, 1.6, 0);
    assertThat(SlingKnockbackModule.findTarget(List.of(far, close), eye, eye.add(0, 0, 5)).getEntity()).isSameAs(close);
    assertThat(SlingKnockbackModule.findTarget(List.of(far), eye, eye.add(5, 0, 0))).isNull();
  }
  @Test
  void crowdedTargetsCannotStealTheDirectlyAimedHit() {
    Entity aimed = target(1);
    Entity beside = mock(Entity.class);
    when(beside.getBoundingBox()).thenReturn(new AABB(0.4, 0, 0, 1, 1.8, 0.6));
    Entity behind = target(-0.5);
    Vec3 eye = new Vec3(0, 1.6, 0);
    for (var entities : List.of(List.of(beside, behind, aimed), List.of(aimed, behind, beside))) {
      var hit = SlingKnockbackModule.findTarget(entities, eye, eye.add(0, 0, 5));
      assertThat(hit.getEntity()).isSameAs(aimed);
    }
  }

  @Test
  void targetsBehindThePlayerAreNotSelectedByExpandedBoxes() {
    Vec3 eye = new Vec3(0, 1.6, 0);
    assertThat(SlingKnockbackModule.findTarget(List.of(target(-0.5)), eye, eye.add(0, 0, 5))).isNull();
  }

  @Test
  void overlappingActualBoxesStillHitFromInside() {
    Entity overlapping = target(0);
    Vec3 eye = new Vec3(0, 1.6, 0);
    assertThat(SlingKnockbackModule.findTarget(List.of(target(2), overlapping), eye, eye.add(0, 0, 5)).getEntity()).isSameAs(overlapping);
  }
}
