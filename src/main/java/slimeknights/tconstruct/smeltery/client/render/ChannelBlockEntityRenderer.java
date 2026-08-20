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
import net.minecraft.core.Direction.Plane;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.client.render.ChannelFluids;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.RenderingHelper;
import slimeknights.tconstruct.smeltery.block.ChannelBlock;
import slimeknights.tconstruct.smeltery.block.ChannelBlock.ChannelConnection;
import slimeknights.tconstruct.smeltery.block.entity.ChannelBlockEntity;

/** Deferred-submit renderer for casting-channel fluid segments. */
public class ChannelBlockEntityRenderer implements BlockEntityRenderer<ChannelBlockEntity,ChannelBlockEntityRenderer.ChannelRenderState> {
  public ChannelBlockEntityRenderer(Context context) {}

  @Override
  public AABB getRenderBoundingBox(ChannelBlockEntity blockEntity) {
    return blockEntity.getRenderBoundingBox();
  }

  @Override
  public ChannelRenderState createRenderState() {
    return new ChannelRenderState();
  }

  @Override
  public void extractRenderState(ChannelBlockEntity channel, ChannelRenderState renderState, float partialTicks,
                                 Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(channel, renderState, partialTicks, cameraPosition, breakProgress);
    renderState.clear();
    FluidStack fluid = channel.getFluid();
    Level level = channel.getLevel();
    if (fluid.isEmpty() || level == null) {
      return;
    }

    BlockPos pos = channel.getBlockPos();
    BlockState blockState = channel.getBlockState();
    ChannelFluids model = ChannelFluids.REGISTRY.get(blockState.getBlock());
    if (model == null) {
      return;
    }

    List<FluidSegment> segments = new ArrayList<>();
    Direction centerFlow = Direction.UP;
    for (Direction direction : Plane.HORIZONTAL) {
      ChannelConnection connection = blockState.getValue(ChannelBlock.DIRECTION_MAP.get(direction));
      if (!connection.canFlow()) {
        continue;
      }
      FluidCuboid cube;
      if (channel.isFlowing(direction)) {
        cube = model.side().flow(connection == ChannelConnection.OUT);
        if (connection == ChannelConnection.OUT) {
          centerFlow = centerFlow == Direction.UP ? direction : centerFlow == direction ? centerFlow : Direction.DOWN;
        }
        if (!level.getBlockState(pos.relative(direction)).is(blockState.getBlock())) {
          segments.add(new FluidSegment(direction, model.side().edge()));
        }
      } else {
        cube = model.side().still();
      }
      segments.add(new FluidSegment(direction, cube));
    }

    if (centerFlow.getAxis().isVertical()) {
      segments.add(new FluidSegment(null, model.center(false)));
    } else {
      segments.add(new FluidSegment(centerFlow, model.center(true)));
    }

    if (blockState.getValue(ChannelBlock.DOWN) && channel.isFlowing(Direction.DOWN)) {
      segments.add(new FluidSegment(null, model.down()));
      renderState.downstream = FaucetBlockEntityRenderer.extractDownstream(level, pos, Direction.DOWN);
    }
    renderState.fluid = fluid.copy();
    renderState.segments = List.copyOf(segments);
  }

  @Override
  public void submit(ChannelRenderState state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
    if (state.fluid.isEmpty()) {
      return;
    }
    for (FluidSegment segment : state.segments) {
      boolean rotated = segment.direction() != null && RenderingHelper.applyRotation(matrices, segment.direction());
      FaucetBlockEntityRenderer.submitCuboids(collector, matrices, List.of(segment.cuboid()), state.fluid, state.lightCoords);
      if (rotated) {
        matrices.popPose();
      }
    }
    for (FaucetBlockEntityRenderer.FluidLayer layer : state.downstream) {
      matrices.pushPose();
      matrices.translate(0, -layer.offset(), 0);
      FaucetBlockEntityRenderer.submitCuboids(collector, matrices, layer.cuboids(), state.fluid, state.lightCoords);
      matrices.popPose();
    }
  }

  record FluidSegment(@Nullable Direction direction, FluidCuboid cuboid) {}

  public static class ChannelRenderState extends BlockEntityRenderState {
    FluidStack fluid = FluidStack.EMPTY;
    List<FluidSegment> segments = List.of();
    List<FaucetBlockEntityRenderer.FluidLayer> downstream = List.of();

    void clear() {
      fluid = FluidStack.EMPTY;
      segments = List.of();
      downstream = List.of();
    }
  }
}
