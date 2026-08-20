package slimeknights.tconstruct.smeltery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.mantle.client.render.MantleRenderTypes;
import slimeknights.tconstruct.client.model.NativeTinkerBlockStateModel;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;
import slimeknights.tconstruct.smeltery.block.entity.ITankBlockEntity;

import java.util.List;

public class TankBlockEntityRenderer<T extends BlockEntity & ITankBlockEntity> implements BlockEntityRenderer<T,TankBlockEntityRenderer.TankRenderState> {
  public TankBlockEntityRenderer(Context context) {}

  @Override
  public TankRenderState createRenderState() {
    return new TankRenderState();
  }

  @Override
  public void extractRenderState(T tile, TankRenderState state, float partialTicks, Vec3 cameraPosition,
                                 ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);

    extractTank(state, tile.getBlockState(), tile.getTank(), partialTicks);
  }

  /** Extracts a live animated tank into state safe for deferred submission. */
  static void extractTank(TankRenderState state, BlockState blockState, FluidTankAnimated tank, float partialTicks) {
    // Only skip the renderer if the native block-state model actually owns
    // the fluid pass. Standard cuboid models still need block-entity fluid.
    if (NativeTinkerBlockStateModel.isNativeTankModel(blockState)) {
      state.clear();
      return;
    }

    List<FluidCuboid> cuboids = FluidCuboid.REGISTRY.get(blockState, List.of());
    FluidStack fluid = tank.getFluid();
    int capacity = tank.getCapacity();
    if (cuboids.isEmpty() || fluid.isEmpty() || capacity <= 0) {
      tank.setRenderOffset(0);
      state.clear();
      return;
    }

    // Preserve the old fill/drain interpolation, but perform it during state
    // extraction. The submit callback must not retain or mutate the live block
    // entity while render nodes may be consumed later in the frame.
    float offset = tank.getRenderOffset();
    if (offset > 1.2f || offset < -1.2f) {
      offset -= (offset / 12f + 0.1f) * partialTicks;
      tank.setRenderOffset(offset);
    } else {
      offset = 0;
      tank.setRenderOffset(0);
    }

    state.cuboids = List.copyOf(cuboids);
    state.fluid = fluid.copy();
    state.capacity = capacity;
    state.offset = offset;
  }

  @Override
  public void submit(TankRenderState state, PoseStack matrices, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
    submitTank(state, matrices, submitNodeCollector);
  }

  /** Submits an extracted tank using the current pose-stack transform. */
  static void submitTank(TankRenderState state, PoseStack matrices, SubmitNodeCollector submitNodeCollector) {
    if (state.fluid.isEmpty() || state.cuboids.isEmpty() || state.capacity <= 0) {
      return;
    }
    submitNodeCollector.submitCustomGeometry(matrices, MantleRenderTypes.FLUID, (pose, buffer) -> {
      for (FluidCuboid cuboid : state.cuboids) {
        FluidRenderer.renderScaledCuboid(pose, buffer, cuboid, state.fluid, state.offset, state.capacity, state.lightCoords, true);
      }
    });
  }

  public static class TankRenderState extends BlockEntityRenderState {
    List<FluidCuboid> cuboids = List.of();
    FluidStack fluid = FluidStack.EMPTY;
    int capacity;
    float offset;

    void clear() {
      cuboids = List.of();
      fluid = FluidStack.EMPTY;
      capacity = 0;
      offset = 0;
    }
  }
}
