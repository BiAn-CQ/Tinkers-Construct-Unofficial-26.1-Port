package slimeknights.tconstruct.library.tools.definition;

import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.json.LevelingValue;
import slimeknights.tconstruct.library.tools.definition.module.weapon.CircleWeaponAttack;
import slimeknights.tconstruct.library.tools.definition.module.weapon.SweepWeaponAttack;

import static org.assertj.core.api.Assertions.assertThat;

class WeaponRangeCompatibilityTest {
  @SuppressWarnings("removal")
  @Test
  void legacyGettersReturnFlatRangeWhileNewGettersKeepScaling() {
    LevelingValue value = new LevelingValue(3, 2);
    CircleWeaponAttack circle = new CircleWeaponAttack(value);
    SweepWeaponAttack sweep = new SweepWeaponAttack(value);
    assertThat(circle.diameter()).isEqualTo(3);
    assertThat(sweep.range()).isEqualTo(3);
    assertThat(circle.diameters()).isSameAs(value);
    assertThat(sweep.ranges()).isSameAs(value);
  }
}
