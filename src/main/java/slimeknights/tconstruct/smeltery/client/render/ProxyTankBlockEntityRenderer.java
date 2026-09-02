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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import org.jspecify.annotations.Nullable;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.RenderItem;
import slimeknights.mantle.client.render.RenderingHelper;
import slimeknights.tconstruct.client.model.NativeTinkerBlockStateModel;
import slimeknights.tconstruct.smeltery.block.entity.ProxyTankBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.tank.ProxyItemTank;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

/** Renderer for {@link ProxyTankBlockEntity}. Unlike {@link TankInventoryBlockEntityRenderer}, does not use a {@link slimeknights.tconstruct.library.fluid.FluidTankAnimated} */
public class ProxyTankBlockEntityRenderer implements BlockEntityRenderer<ProxyTankBlockEntity,ProxyTankBlockEntityRenderer.ProxyTankRenderState> {
  private final ItemModelResolver itemModelResolver;

  public ProxyTankBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    this.itemModelResolver = context.itemModelResolver();
  }

  @Override
  public ProxyTankRenderState createRenderState() {
    return new ProxyTankRenderState();
  }

  @Override
  public void extractRenderState(ProxyTankBlockEntity proxyTank, ProxyTankRenderState renderState, float partialTicks,
                                 Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(proxyTank, renderState, partialTicks, cameraPosition, breakProgress);
    BlockState blockState = proxyTank.getBlockState();
    renderState.facing = blockState.getValue(HORIZONTAL_FACING);
    renderState.clear();

    ProxyItemTank<?> itemTank = proxyTank.getItemTank();
    if (!NativeTinkerBlockStateModel.isNativeTankModel(blockState)) {
      renderState.cuboids = List.copyOf(FluidCuboid.REGISTRY.get(blockState, List.of()));
      ResourceHandler<FluidResource> handler = itemTank.getFluidHandler();
      renderState.fluid = handler.size() == 0 ? FluidStack.EMPTY : FluidUtil.getStack(handler, 0).copy();
      renderState.capacity = handler.size() == 0 ? 0 : handler.getCapacityAsInt(0, handler.getResource(0));
    }

    List<RenderItem> placements = RenderItem.STATE_REGISTRY.get(blockState, List.of());
    List<ItemStackRenderState> items = new ArrayList<>(placements.size());
    int seed = (int)proxyTank.getBlockPos().asLong();
    for (int slot = 0; slot < placements.size(); slot++) {
      ItemStackRenderState itemState = new ItemStackRenderState();
      RenderItem placement = placements.get(slot);
      if (!placement.isHidden() && slot < itemTank.size()) {
        itemModelResolver.updateForTopItem(itemState, ItemUtil.getStack(itemTank, slot), placement.getTransform(),
                                           proxyTank.getLevel(), null, seed + slot);
      }
      items.add(itemState);
    }
    renderState.placements = List.copyOf(placements);
    renderState.items = items;
  }

  @Override
  public void submit(ProxyTankRenderState state, PoseStack matrices, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
    boolean isRotated = RenderingHelper.applyRotation(matrices, state.facing);
    TankBlockEntityRenderer.submitTank(state, matrices, submitNodeCollector);
    for (int slot = 0; slot < state.items.size(); slot++) {
      RenderingHelper.submitItem(matrices, submitNodeCollector, state.items.get(slot), state.placements.get(slot), state.lightCoords);
    }
    if (isRotated) {
      matrices.popPose();
    }
  }

  public static class ProxyTankRenderState extends TankBlockEntityRenderer.TankRenderState {
    net.minecraft.core.Direction facing = net.minecraft.core.Direction.SOUTH;
    List<RenderItem> placements = List.of();
    List<ItemStackRenderState> items = List.of();
  }
}
