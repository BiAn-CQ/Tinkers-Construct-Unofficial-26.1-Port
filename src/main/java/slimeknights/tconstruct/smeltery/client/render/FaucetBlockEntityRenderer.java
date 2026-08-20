package slimeknights.tconstruct.smeltery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.client.render.FaucetFluid;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.mantle.client.render.MantleRenderTypes;
import slimeknights.mantle.client.render.RenderingHelper;
import slimeknights.tconstruct.smeltery.block.FaucetBlock;
import slimeknights.tconstruct.smeltery.block.entity.FaucetBlockEntity;

/** Deferred-submit renderer for faucet fluid and the stream below it. */
public class FaucetBlockEntityRenderer implements BlockEntityRenderer<FaucetBlockEntity,FaucetBlockEntityRenderer.FaucetRenderState> {
  public FaucetBlockEntityRenderer(Context context) {}

  @Override
  public AABB getRenderBoundingBox(FaucetBlockEntity blockEntity) {
    return blockEntity.getRenderBoundingBox();
  }

  @Override
  public FaucetRenderState createRenderState() {
    return new FaucetRenderState();
  }

  @Override
  public void extractRenderState(FaucetBlockEntity faucet, FaucetRenderState state, float partialTicks, Vec3 cameraPosition,
                                 ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(faucet, state, partialTicks, cameraPosition, breakProgress);
    state.clear();
    FluidStack fluid = faucet.getRenderFluid();
    Level level = faucet.getLevel();
    if (!faucet.isPouring() || fluid.isEmpty() || level == null) {
      return;
    }

    BlockState blockState = faucet.getBlockState();
    state.fluid = fluid.copy();
    state.facing = blockState.getValue(FaucetBlock.FACING);
    state.cuboids = List.copyOf(FluidCuboid.REGISTRY.get(blockState, List.of()));
    state.downstream = extractDownstream(level, faucet.getBlockPos(), state.facing);
  }

  /** Snapshots all resource-pack-defined stream segments below an outlet. */
  static List<FluidLayer> extractDownstream(Level level, BlockPos pos, Direction direction) {
    List<FluidLayer> layers = new ArrayList<>();
    int offset = 0;
    FaucetFluid faucetFluid;
    do {
      offset++;
      faucetFluid = FaucetFluid.REGISTRY.get(level.getBlockState(pos.below(offset)));
      List<FluidCuboid> cuboids = faucetFluid.getFluids(direction);
      if (!cuboids.isEmpty()) {
        layers.add(new FluidLayer(offset, List.copyOf(cuboids)));
      }
    } while (faucetFluid.isContinued());
    return List.copyOf(layers);
  }

  static void submitCuboids(SubmitNodeCollector collector, PoseStack matrices, List<FluidCuboid> cuboids,
                            FluidStack fluid, int light) {
    if (!cuboids.isEmpty()) {
      collector.submitCustomGeometry(matrices, MantleRenderTypes.FLUID,
        (pose, buffer) -> FluidRenderer.renderCuboids(pose, buffer, cuboids, fluid, light, false));
    }
  }

  @Override
  public void submit(FaucetRenderState state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
    if (state.fluid.isEmpty()) {
      return;
    }
    boolean rotated = RenderingHelper.applyRotation(matrices, state.facing);
    submitCuboids(collector, matrices, state.cuboids, state.fluid, state.lightCoords);
    for (FluidLayer layer : state.downstream) {
      matrices.pushPose();
      matrices.translate(0, -layer.offset(), 0);
      submitCuboids(collector, matrices, layer.cuboids(), state.fluid, state.lightCoords);
      matrices.popPose();
    }
    if (rotated) {
      matrices.popPose();
    }
  }

  record FluidLayer(int offset, List<FluidCuboid> cuboids) {}

  public static class FaucetRenderState extends BlockEntityRenderState {
    FluidStack fluid = FluidStack.EMPTY;
    Direction facing = Direction.DOWN;
    List<FluidCuboid> cuboids = List.of();
    List<FluidLayer> downstream = List.of();

    void clear() {
      fluid = FluidStack.EMPTY;
      facing = Direction.DOWN;
      cuboids = List.of();
      downstream = List.of();
    }
  }
}
