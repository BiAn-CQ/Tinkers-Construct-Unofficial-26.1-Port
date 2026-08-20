package slimeknights.tconstruct.library.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ModifiableItemClientExtensionTest {
  @Test
  void blockUsesOnlyTheBaseItemArmTransform() {
    PoseStack poseStack = mock(PoseStack.class);
    LocalPlayer player = activeMainHandPlayer();
    ItemStack stack = mock(ItemStack.class);
    when(stack.getUseAnimation()).thenReturn(ItemUseAnimation.BLOCK);

    assertThat(ModifiableItemClientExtension.INSTANCE.applyForgeHandTransform(
      poseStack, player, HumanoidArm.RIGHT, stack, 0.5f, 0.25f, 0.0f)).isTrue();
    verify(poseStack).translate(0.56f, -0.52f + 0.25f * -0.6f, -0.72f);
  }

  @Test
  void otherAnimationsRemainHandledByMinecraft() {
    PoseStack poseStack = mock(PoseStack.class);
    LocalPlayer player = activeMainHandPlayer();
    ItemStack stack = mock(ItemStack.class);
    when(stack.getUseAnimation()).thenReturn(ItemUseAnimation.BOW);

    assertThat(ModifiableItemClientExtension.INSTANCE.applyForgeHandTransform(
      poseStack, player, HumanoidArm.RIGHT, stack, 0.5f, 0.25f, 0.0f)).isFalse();
    verifyNoInteractions(poseStack);
  }

  private static LocalPlayer activeMainHandPlayer() {
    LocalPlayer player = mock(LocalPlayer.class);
    when(player.getMainArm()).thenReturn(HumanoidArm.RIGHT);
    when(player.isUsingItem()).thenReturn(true);
    when(player.getUseItemRemainingTicks()).thenReturn(20);
    when(player.getUsedItemHand()).thenReturn(InteractionHand.MAIN_HAND);
    return player;
  }
}
