package slimeknights.tconstruct.tools.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.mantle.client.render.MantleRenderTypes;
import slimeknights.tconstruct.tools.entity.FluidEffectProjectile;

import java.util.List;

public class FluidEffectProjectileRenderer extends EntityRenderer<FluidEffectProjectile, FluidEffectProjectileRenderer.State> {
  private final List<FluidCuboid> fluids;
  public FluidEffectProjectileRenderer(Context context) {
    super(context);
    this.fluids = List.of(
      FluidCuboid.builder().from(-4,  0,  0).to(-2,  2,  2).build(),
      FluidCuboid.builder().from( 0, -4,  0).to( 2, -2,  2).build(),
      FluidCuboid.builder().from( 0,  0, -4).to( 2,  2, -2).build(),
      FluidCuboid.builder().from( 2,  0,  0).to( 4,  2,  2).build(),
      FluidCuboid.builder().from( 0,  0,  0).to( 2,  4,  2).build(),
      FluidCuboid.builder().from( 0,  0,  2).to( 2,  2,  4).build());
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(FluidEffectProjectile entity, State state, float partialTicks) {
    super.extractRenderState(entity, state, partialTicks);
    state.yRot = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F;
    state.xRot = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
    state.fluid = entity.getFluid().copy();
  }

  @Override
  public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
    if (!state.fluid.isEmpty()) {
      poseStack.pushPose();
      poseStack.translate(0.0D, 0.15F, 0.0D);
      poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot));
      poseStack.mulPose(Axis.ZP.rotationDegrees(state.xRot));
      submitNodeCollector.submitCustomGeometry(poseStack, MantleRenderTypes.FLUID,
        (pose, buffer) -> FluidRenderer.renderCuboids(pose, buffer, fluids, state.fluid, state.lightCoords));
      poseStack.popPose();
    }
    super.submit(state, poseStack, submitNodeCollector, camera);
  }

  public static class State extends EntityRenderState {
    private float yRot;
    private float xRot;
    private FluidStack fluid = FluidStack.EMPTY;
  }
}
