package slimeknights.tconstruct.smeltery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.client.render.RenderItem;
import slimeknights.mantle.client.render.RenderingHelper;
import slimeknights.tconstruct.smeltery.block.entity.ITankBlockEntity.ITankInventoryBlockEntity;

public class TankInventoryBlockEntityRenderer<T extends BlockEntity & ITankInventoryBlockEntity>
  implements BlockEntityRenderer<T,TankInventoryBlockEntityRenderer.TankInventoryRenderState> {
  private final EnumProperty<Direction> directionProperty;
  private final ItemModelResolver itemModelResolver;

  public TankInventoryBlockEntityRenderer(BlockEntityRendererProvider.Context context, EnumProperty<Direction> directionProperty) {
    this.directionProperty = directionProperty;
    this.itemModelResolver = context.itemModelResolver();
  }

  @Override
  public TankInventoryRenderState createRenderState() {
    return new TankInventoryRenderState();
  }

  @Override
  public void extractRenderState(T melter, TankInventoryRenderState state, float partialTicks, Vec3 cameraPosition,
                                 ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(melter, state, partialTicks, cameraPosition, breakProgress);
    BlockState blockState = melter.getBlockState();
    state.facing = blockState.getValue(directionProperty);
    TankBlockEntityRenderer.extractTank(state, blockState, melter.getTank(), partialTicks);

    List<RenderItem> placements = RenderItem.STATE_REGISTRY.get(blockState, List.of());
    List<ItemStackRenderState> items = new ArrayList<>(placements.size());
    int seed = (int)melter.getBlockPos().asLong();
    for (int slot = 0; slot < placements.size(); slot++) {
      ItemStackRenderState itemState = new ItemStackRenderState();
      RenderItem placement = placements.get(slot);
      if (!placement.isHidden() && slot < melter.getItemHandler().getSlots()) {
        itemModelResolver.updateForTopItem(itemState, melter.getItemHandler().getStackInSlot(slot), placement.getTransform(),
                                           melter.getLevel(), null, seed + slot);
      }
      items.add(itemState);
    }
    state.placements = List.copyOf(placements);
    state.items = items;
  }

  @Override
  public void submit(TankInventoryRenderState state, PoseStack matrices, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
    boolean isRotated = RenderingHelper.applyRotation(matrices, state.facing);
    TankBlockEntityRenderer.submitTank(state, matrices, submitNodeCollector);
    for (int slot = 0; slot < state.items.size(); slot++) {
      RenderingHelper.submitItem(matrices, submitNodeCollector, state.items.get(slot), state.placements.get(slot), state.lightCoords);
    }
    if (isRotated) {
      matrices.popPose();
    }
  }

  public static class TankInventoryRenderState extends TankBlockEntityRenderer.TankRenderState {
    Direction facing = Direction.SOUTH;
    List<RenderItem> placements = List.of();
    List<ItemStackRenderState> items = List.of();
  }
}
