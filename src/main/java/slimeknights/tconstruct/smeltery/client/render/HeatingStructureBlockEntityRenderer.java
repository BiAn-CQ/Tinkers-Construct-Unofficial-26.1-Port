package slimeknights.tconstruct.smeltery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.TinkerItemDisplays;
import slimeknights.tconstruct.smeltery.block.controller.ControllerBlock;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModuleInventory;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.HeatingStructureMultiblock.StructureData;

/** Extracted-state renderer for smeltery/foundry interiors and error outlines. */
public class HeatingStructureBlockEntityRenderer
  implements BlockEntityRenderer<HeatingStructureBlockEntity,HeatingStructureBlockEntityRenderer.HeatingStructureRenderState> {
  private static final float ITEM_SCALE = 15f / 16f;
  private static final int ESTIMATED_ITEM_QUADS = 26;
  private final ItemModelResolver itemModelResolver;

  public HeatingStructureBlockEntityRenderer(Context context) {
    this.itemModelResolver = context.itemModelResolver();
  }

  @Override
  public AABB getRenderBoundingBox(HeatingStructureBlockEntity blockEntity) {
    return blockEntity.getRenderBoundingBox();
  }

  @Override
  public HeatingStructureRenderState createRenderState() {
    return new HeatingStructureRenderState();
  }

  @Override
  public void extractRenderState(HeatingStructureBlockEntity smeltery, HeatingStructureRenderState state, float partialTicks,
                                 Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(smeltery, state, partialTicks, cameraPosition, breakProgress);
    state.clear();
    Level level = smeltery.getLevel();
    if (level == null) {
      return;
    }
    BlockState blockState = smeltery.getBlockState();
    StructureData structure = smeltery.getStructure();
    state.structureValid = blockState.getValue(ControllerBlock.IN_STRUCTURE) && structure != null;
    extractErrorOutline(smeltery, state, state.structureValid);
    if (!state.structureValid) {
      return;
    }

    BlockPos controllerPos = smeltery.getBlockPos();
    BlockPos minPos = structure.getMinInside();
    BlockPos maxPos = structure.getMaxInside();
    state.offsetX = minPos.getX() - controllerPos.getX();
    state.offsetY = minPos.getY() - controllerPos.getY();
    state.offsetZ = minPos.getZ() - controllerPos.getZ();
    state.minPos = minPos;
    state.maxPos = maxPos;
    state.fluidLight = getLightColor(level, minPos);
    state.fluids = smeltery.getTank().getFluids().stream().map(FluidStack::copy).toList();
    state.capacity = smeltery.getTank().getCapacity();
    state.facing = blockState.getValue(ControllerBlock.FACING);

    int maxQuads = Config.CLIENT.maxSmelteryItemQuads.get();
    if (maxQuads == 0) {
      return;
    }
    int xd = 1 + maxPos.getX() - minPos.getX();
    int zd = 1 + maxPos.getZ() - minPos.getZ();
    int layerSize = xd * zd;
    MeltingModuleInventory inventory = smeltery.getMeltingInventory();
    List<MeltingItemState> items = new ArrayList<>();
    int estimatedQuads = 0;
    for (int slot = 0; slot < inventory.getSlots(); slot++) {
      ItemStack stack = inventory.getStackInSlot(slot);
      if (stack.isEmpty()) {
        continue;
      }
      int height = slot / layerSize;
      int layerIndex = slot % layerSize;
      int x = layerIndex % xd;
      int z = layerIndex / xd;
      BlockPos itemPos = minPos.offset(x, height, z);
      ItemStackRenderState itemState = new ItemStackRenderState();
      itemModelResolver.updateForTopItem(itemState, stack, TinkerItemDisplays.MELTER, level, null, slot);
      items.add(new MeltingItemState(itemState, x, height, z, getLightColor(level, itemPos)));
      if (maxQuads != -1 && (estimatedQuads += ESTIMATED_ITEM_QUADS) > maxQuads) {
        break;
      }
    }
    state.items = List.copyOf(items);
  }

  /** Packs block and sky light using the vanilla lightmap coordinate layout. */
  private static int getLightColor(Level level, BlockPos pos) {
    return level.getBrightness(LightLayer.BLOCK, pos) << 4 | level.getBrightness(LightLayer.SKY, pos) << 20;
  }

  private static void extractErrorOutline(HeatingStructureBlockEntity smeltery, HeatingStructureRenderState state,
                                          boolean structureValid) {
    BlockPos errorPos = smeltery.getErrorPos();
    Player player = Minecraft.getInstance().player;
    if (errorPos == null || player == null) {
      return;
    }
    boolean highlightError = smeltery.isHighlightError();
    if ((structureValid || !highlightError) && !smeltery.showDebugBlockBorder(player)) {
      return;
    }
    BlockPos controllerPos = smeltery.getBlockPos();
    BlockPos playerPos = player.blockPosition();
    int dx = playerPos.getX() - controllerPos.getX();
    int dz = playerPos.getZ() - controllerPos.getZ();
    if (dx * dx + dz * dz >= 512) {
      return;
    }
    state.errorX = errorPos.getX() - controllerPos.getX();
    state.errorY = errorPos.getY() - controllerPos.getY();
    state.errorZ = errorPos.getZ() - controllerPos.getZ();
    state.errorRenderType = highlightError ? RenderTypes.LINES_TRANSLUCENT : RenderTypes.LINES;
    state.errorColor = structureValid ? ARGB.color(0xFF, 0xFF, 0xFF, 0) : ARGB.color(0xFF, 0xFF, 0, 0);
  }

  @Override
  public void submit(HeatingStructureRenderState state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
    if (state.errorRenderType != null) {
      matrices.pushPose();
      matrices.translate(state.errorX, state.errorY, state.errorZ);
      collector.submitCustomGeometry(matrices, state.errorRenderType, (pose, buffer) -> {
        PoseStack local = new PoseStack();
        local.last().set(pose);
        ShapeRenderer.renderShape(local, buffer, Shapes.block(), 0, 0, 0, state.errorColor, 0.5f);
      });
      matrices.popPose();
    }
    if (!state.structureValid) {
      return;
    }

    matrices.pushPose();
    matrices.translate(state.offsetX, state.offsetY, state.offsetZ);
    SmelteryTankRenderer.submitFluids(matrices, collector, state.fluids, state.capacity,
                                      state.minPos, state.maxPos, state.fluidLight);
    for (MeltingItemState item : state.items) {
      matrices.pushPose();
      matrices.translate(item.x() + 0.5f, item.y() + 0.5f, item.z() + 0.5f);
      matrices.mulPose(Axis.YP.rotationDegrees(-90f * state.facing.get2DDataValue()));
      matrices.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
      item.item().submit(matrices, collector, item.light(), net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0);
      matrices.popPose();
    }
    matrices.popPose();
  }

  @Override
  public boolean shouldRenderOffScreen() {
    return true;
  }

  record MeltingItemState(ItemStackRenderState item, int x, int y, int z, int light) {}

  public static class HeatingStructureRenderState extends BlockEntityRenderState {
    boolean structureValid;
    int offsetX;
    int offsetY;
    int offsetZ;
    BlockPos minPos = BlockPos.ZERO;
    BlockPos maxPos = BlockPos.ZERO;
    List<FluidStack> fluids = List.of();
    int capacity;
    int fluidLight;
    Direction facing = Direction.SOUTH;
    List<MeltingItemState> items = List.of();
    @Nullable RenderType errorRenderType;
    int errorColor;
    int errorX;
    int errorY;
    int errorZ;

    void clear() {
      structureValid = false;
      fluids = List.of();
      capacity = 0;
      items = List.of();
      errorRenderType = null;
    }
  }
}
