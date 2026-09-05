package slimeknights.tconstruct.mixin.client;

import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import slimeknights.tconstruct.client.FancyArmorStandRendererCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Small armor stands use scaled adult armor, not the humanoid baby texture layout. */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {
  @Redirect(
    method = "renderArmorPiece",
    at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;isBaby:Z")
  )
  private boolean tconstruct$useBabyArmorTexture(HumanoidRenderState state) {
    // Leave getArmorModel unchanged so small stands still use the small model.
    return FancyArmorStandRendererCompat.usesBabyArmorTextures(state);
  }
}
