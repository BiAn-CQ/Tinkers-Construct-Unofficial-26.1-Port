package slimeknights.tconstruct.library.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;

/** Client extensions for modifiable items. Used in non-armor items to adjust first-person animations. */
public class ModifiableItemClientExtension implements IClientItemExtensions {
  public static final ModifiableItemClientExtension INSTANCE = new ModifiableItemClientExtension();

  protected ModifiableItemClientExtension() {}

  /** Static copy of the base item arm transform used by the vanilla first-person renderer. */
  private static void applyItemArmTransform(PoseStack poseStack, float equippedProgress, int sideOffset) {
    poseStack.translate(sideOffset * 0.56f, -0.52f + equippedProgress * -0.6f, -0.72f);
  }

  @Override
  public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack stack, float partialTicks, float equipProgress, float swingProgress) {
    InteractionHand hand = arm == player.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    if (!player.isUsingItem() || player.getUseItemRemainingTicks() <= 0 || player.getUsedItemHand() != hand) {
      return false;
    }

    int sideOffset = arm == HumanoidArm.RIGHT ? 1 : -1;
    ItemUseAnimation animation = stack.getUseAnimation();
    if (animation == ItemUseAnimation.BLOCK) {
      // Tinkers' blocking models already contain their shield-specific display transform. Minecraft 26.1
      // adds an additional non-ShieldItem block rotation after the base arm transform, which double-rotates
      // modifiable shields and battlesigns. Handle BLOCK here to preserve their 1.20.1 rendering contract.
      applyItemArmTransform(poseStack, equipProgress, sideOffset);
      return true;
    }
    if (animation != ItemUseAnimation.TRIDENT) {
      return false;
    }

    applyItemArmTransform(poseStack, equipProgress, sideOffset);
    poseStack.translate(sideOffset * -0.5f, 0.7f, 0.1f);
    // Tinkers' throwing tools are diagonal item sprites, unlike a vanilla
    // trident. The extra 35 degrees restores the 1.20.1 throwing pose.
    poseStack.mulPose(Axis.XP.rotationDegrees(-90));
    poseStack.mulPose(Axis.YP.rotationDegrees(sideOffset * 35.3f));
    poseStack.mulPose(Axis.ZP.rotationDegrees(sideOffset * -9.785f));

    float remainingTime = stack.getUseDuration(player) - (player.getUseItemRemainingTicks() - partialTicks + 1);
    float charge = remainingTime / Math.max(1,
      ModifierUtil.getPersistentInt(stack, GeneralInteractionModifierHook.KEY_DRAWTIME, 20));
    charge = Math.min(charge, 1);
    if (charge > 0.1f) {
      poseStack.translate(0, Mth.sin((remainingTime - 0.1f) * 1.3f) * (charge - 0.1f) * 0.004f, 0);
    }
    poseStack.translate(0, 0, charge * 0.2f);
    poseStack.scale(1, 1, 1 + charge * 0.2f);
    poseStack.mulPose(Axis.YN.rotationDegrees(sideOffset * 45));
    return true;
  }
}
