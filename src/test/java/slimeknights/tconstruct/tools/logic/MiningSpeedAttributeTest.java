package slimeknights.tconstruct.tools.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.modifiers.hook.mining.BreakSpeedContext;
import slimeknights.tconstruct.test.CoreTestBootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MiningSpeedAttributeTest extends CoreTestBootstrap {
  @Test
  void additiveMiningBonusesFollowTheNativeAttributeAndHaste() {
    var player = player(1.5);
    when(player.hasEffect(MobEffects.HASTE)).thenReturn(true);
    when(player.getEffect(MobEffects.HASTE)).thenReturn(new MobEffectInstance(MobEffects.HASTE, 100, 0));

    assertThat(BreakSpeedContext.getMiningModifier(player)).isCloseTo(1.8f, within(0.0001f));
  }

  @Test
  void zeroNativeBreakSpeedAlsoDisablesAdditiveBonuses() {
    assertThat(BreakSpeedContext.getMiningModifier(player(0))).isZero();
  }

  @Test
  void breakSpeedEventDoesNotApplyTheNativeMultiplierAgain() {
    var player = player(1.5);
    when(player.getMainHandItem()).thenReturn(ItemStack.EMPTY);
    var event = new PlayerEvent.BreakSpeed(player, Blocks.STONE.defaultBlockState(), 15, BlockPos.ZERO);

    ToolEvents.onBreakSpeed(event);

    assertThat(event.getNewSpeed()).isEqualTo(15);
  }

  private static Player player(double multiplier) {
    var player = mock(Player.class);
    var attribute = new AttributeInstance(Attributes.BLOCK_BREAK_SPEED, ignored -> {});
    attribute.setBaseValue(multiplier);
    when(player.getAttribute(Attributes.BLOCK_BREAK_SPEED)).thenReturn(attribute);
    when(player.onGround()).thenReturn(true);
    return player;
  }
}
