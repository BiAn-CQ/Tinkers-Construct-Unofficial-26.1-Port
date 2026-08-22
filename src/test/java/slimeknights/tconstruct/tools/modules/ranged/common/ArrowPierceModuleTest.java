package slimeknights.tconstruct.tools.modules.ranged.common;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.json.LevelingInt;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.mixin.AbstractArrowAccessor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class ArrowPierceModuleTest {
  @Test
  @SuppressWarnings("unchecked")
  void addsPiercingToAnyAbstractArrow() {
    IToolStackView tool = mock(IToolStackView.class);
    ModifierEntry modifier = mock(ModifierEntry.class);
    ModifierCondition<IToolStackView> condition = mock(ModifierCondition.class);
    AbstractArrow arrow = mock(AbstractArrow.class, withSettings().extraInterfaces(AbstractArrowAccessor.class));

    when(condition.matches(tool, modifier)).thenReturn(true);
    when(modifier.getEffectiveLevel()).thenReturn(2f);
    when(arrow.getPierceLevel()).thenReturn((byte)1);

    new ArrowPierceModule(LevelingInt.eachLevel(1), condition)
      .onProjectileShoot(tool, modifier, null, null, arrow, arrow, null, true);

    verify((AbstractArrowAccessor)arrow).tconstruct$setPierceLevel((byte)3);
  }
}
