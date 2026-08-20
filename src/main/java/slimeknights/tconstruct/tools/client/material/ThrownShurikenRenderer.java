package slimeknights.tconstruct.tools.client.material;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import slimeknights.tconstruct.tools.entity.ToolProjectile;

/** Renderer for spinning shuriken-style {@link ToolProjectile} entities. */
public class ThrownShurikenRenderer<E extends Entity & ToolProjectile> extends EntityRenderer<E, ThrownShurikenRenderer.State> {
  private final ItemModelResolver itemModelResolver;
  public ThrownShurikenRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.itemModelResolver = context.getItemModelResolver();
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(E entity, State state, float partialTicks) {
    super.extractRenderState(entity, state, partialTicks);
    state.yRot = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) + 90;
    state.age = entity.tickCount + partialTicks;
    state.visible = entity.tickCount >= 2 || this.entityRenderDispatcher.distanceToSqr(entity) >= 12.25D;
    itemModelResolver.updateForNonLiving(state.item, entity.getDisplayTool(), ItemDisplayContext.GROUND, entity);
  }

  @Override
  public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
    if (state.visible) {
      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot));
      poseStack.mulPose(Axis.ZP.rotationDegrees(state.age * 30 % 360));
      poseStack.translate(-0.03125, -0.09375, 0);
      state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
      poseStack.popPose();
    }
    super.submit(state, poseStack, submitNodeCollector, camera);
  }

  public static class State extends ThrownItemRenderState {
    private float yRot;
    private float age;
    private boolean visible;
  }
}
