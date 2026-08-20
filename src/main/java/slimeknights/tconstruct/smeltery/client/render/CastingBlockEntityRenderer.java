package slimeknights.tconstruct.smeltery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.mantle.client.render.MantleRenderTypes;
import slimeknights.mantle.client.render.RenderItem;
import slimeknights.mantle.client.render.RenderingHelper;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.tank.CastingFluidHandler;
import slimeknights.tconstruct.smeltery.client.util.TintedSubmitNodeCollector;

/** 26.1 extracted-state renderer for casting tables and basins. */
public class CastingBlockEntityRenderer implements BlockEntityRenderer<CastingBlockEntity,CastingBlockEntityRenderer.CastingRenderState> {
  private final ItemModelResolver itemModelResolver;

  public CastingBlockEntityRenderer(Context context) {
    this.itemModelResolver = context.itemModelResolver();
  }

  @Override
  public CastingRenderState createRenderState() {
    return new CastingRenderState();
  }

  @Override
  public void extractRenderState(CastingBlockEntity casting, CastingRenderState state, float partialTicks,
                                 Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(casting, state, partialTicks, cameraPosition, breakProgress);
    state.clear();
    BlockState blockState = casting.getBlockState();
    state.blockState = blockState;
    state.fluidCuboids = List.copyOf(FluidCuboid.REGISTRY.get(blockState, List.of()));
    state.placements = List.copyOf(RenderItem.STATE_REGISTRY.get(blockState, List.of()));

    int timer = casting.getTimer();
    int totalTime = casting.getCoolingTime();
    if (timer > 0 && totalTime > 0) {
      int opacity = 4 * 0xFF * timer / totalTime;
      state.itemOpacity = opacity / 4;
      if (opacity > 3 * 0xFF) {
        state.fluidOpacity = 4 * 0xFF - opacity;
      }
    }

    CastingFluidHandler tank = casting.getTank();
    state.fluid = tank.getFluid().copy();
    state.capacity = tank.getCapacity();
    state.fluidFull = state.capacity > 0 && state.fluid.getAmount() == state.capacity;

    int seed = (int)casting.getBlockPos().asLong();
    if (!state.placements.isEmpty() && !state.placements.get(0).isHidden()) {
      state.input = resolve(casting.getItem(0), state.placements.get(0), casting, seed);
    }
    if (state.placements.size() >= 2 && !state.placements.get(1).isHidden()) {
      ItemStack output = casting.getItem(1);
      if (state.itemOpacity > 0 && output.isEmpty()) {
        output = casting.getRecipeOutput();
        state.tintOutput = !output.isEmpty();
      }
      state.output = resolve(output, state.placements.get(1), casting, seed + 1);
    }
  }

  private ItemStackRenderState resolve(ItemStack stack, RenderItem placement, CastingBlockEntity casting, int seed) {
    ItemStackRenderState itemState = new ItemStackRenderState();
    itemModelResolver.updateForTopItem(itemState, stack, placement.getTransform(), casting.getLevel(), null, seed);
    return itemState;
  }

  @Override
  public void submit(CastingRenderState state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
    boolean rotated = RenderingHelper.applyRotation(matrices, state.blockState);
    if (!state.fluid.isEmpty() && !state.fluidCuboids.isEmpty() && state.capacity > 0) {
      collector.submitCustomGeometry(matrices, MantleRenderTypes.FLUID, (pose, buffer) -> {
        if (state.fluidFull) {
          FluidRenderer.renderCuboids(pose, buffer, state.fluidCuboids, state.fluid, state.lightCoords, state.fluidOpacity);
        } else {
          for (FluidCuboid cuboid : state.fluidCuboids) {
            FluidRenderer.renderScaledCuboid(pose, buffer, cuboid, state.fluid, 0, state.capacity, state.lightCoords, false);
          }
        }
      });
    }

    if (!state.placements.isEmpty()) {
      RenderingHelper.submitItem(matrices, collector, state.input, state.placements.get(0), state.lightCoords);
    }
    if (state.placements.size() >= 2) {
      SubmitNodeCollector outputCollector = state.tintOutput
        ? new TintedSubmitNodeCollector(collector, state.itemOpacity, state.fluidOpacity)
        : collector;
      RenderingHelper.submitItem(matrices, outputCollector, state.output, state.placements.get(1), state.lightCoords);
    }
    if (rotated) {
      matrices.popPose();
    }
  }

  public static class CastingRenderState extends BlockEntityRenderState {
    BlockState blockState;
    List<FluidCuboid> fluidCuboids = List.of();
    List<RenderItem> placements = List.of();
    FluidStack fluid = FluidStack.EMPTY;
    int capacity;
    boolean fluidFull;
    int itemOpacity;
    int fluidOpacity = 0xFF;
    boolean tintOutput;
    ItemStackRenderState input = new ItemStackRenderState();
    ItemStackRenderState output = new ItemStackRenderState();

    void clear() {
      fluidCuboids = List.of();
      placements = List.of();
      fluid = FluidStack.EMPTY;
      capacity = 0;
      fluidFull = false;
      itemOpacity = 0;
      fluidOpacity = 0xFF;
      tintOutput = false;
      input = new ItemStackRenderState();
      output = new ItemStackRenderState();
    }
  }
}
