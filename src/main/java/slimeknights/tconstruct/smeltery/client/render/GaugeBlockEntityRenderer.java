package slimeknights.tconstruct.smeltery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.mantle.client.render.MantleRenderTypes;
import slimeknights.tconstruct.smeltery.block.entity.GaugeBlockEntity;

/** Renderer for the obisidian gauge block */
public class GaugeBlockEntityRenderer implements BlockEntityRenderer<GaugeBlockEntity,GaugeBlockEntityRenderer.GaugeRenderState> {
  public GaugeBlockEntityRenderer(Context context) {}

  @Override
  public GaugeRenderState createRenderState() {
    return new GaugeRenderState();
  }

  @Override
  public void extractRenderState(GaugeBlockEntity tile, GaugeRenderState state, float partialTicks, Vec3 cameraPosition,
                                 ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
    state.cuboids = List.copyOf(FluidCuboid.REGISTRY.get(tile.getBlockState(), List.of()));
    ResourceHandler<FluidResource> tank = tile.getTank();
    state.fluid = tank.size() > 0 ? FluidUtil.getStack(tank, 0) : FluidStack.EMPTY;
  }

  @Override
  public void submit(GaugeRenderState state, PoseStack matrices, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
    if (!state.cuboids.isEmpty() && !state.fluid.isEmpty()) {
      submitNodeCollector.submitCustomGeometry(matrices, MantleRenderTypes.FLUID,
        (pose, buffer) -> FluidRenderer.renderCuboids(pose, buffer, state.cuboids, state.fluid, state.lightCoords));
    }
  }

  public static class GaugeRenderState extends BlockEntityRenderState {
    List<FluidCuboid> cuboids = List.of();
    FluidStack fluid = FluidStack.EMPTY;
  }
}
