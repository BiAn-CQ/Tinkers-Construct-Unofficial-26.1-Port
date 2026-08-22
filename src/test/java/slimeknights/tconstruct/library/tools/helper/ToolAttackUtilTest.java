package slimeknights.tconstruct.library.tools.helper;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ToolAttackUtilTest {
  @Test
  void getToolAttributeKeepsToolValueWhenAttributeInstanceIsUnavailable() {
    IToolStackView tool = mock(IToolStackView.class);
    LivingEntity holder = mock(LivingEntity.class);
    when(holder.getAttribute(Attributes.ATTACK_DAMAGE)).thenReturn(null);
    when(holder.getAttributeBaseValue(Attributes.ATTACK_DAMAGE)).thenReturn(1.0);

    float damage = ToolAttackUtil.getToolAttribute(tool, holder, Attributes.ATTACK_DAMAGE, 5.0f);

    assertThat(damage).isEqualTo(6.0f);
  }
}
