package slimeknights.tconstruct.tools.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.aoe.AreaOfEffectIterator;
import slimeknights.tconstruct.library.tools.definition.module.aoe.AreaOfEffectIterator.AOEMatchType;
import slimeknights.tconstruct.library.tools.definition.module.mining.IsEffectiveToolHook;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.utils.BlockSideHitListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@net.neoforged.fml.common.EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT)
public class ToolRenderEvents {
  /** Maximum number of blocks from the iterator to render */
  private static final int MAX_BLOCKS = 60;

  /**
   * Renders the outline on the extra blocks
   *
   * @param event the highlight event
   */
  @SubscribeEvent
  static void renderBlockHighlights(ExtractBlockOutlineRenderStateEvent event) {
    Level world = Minecraft.getInstance().level;
    Player player = Minecraft.getInstance().player;
    if (world == null || player == null) {
      return;
    }
    // must have the right tags
    ItemStack stack = player.getMainHandItem();
    if (stack.isEmpty() || !stack.is(TinkerTags.Items.MODIFIABLE)) {
      return;
    }
    // must be targeting a block
    HitResult result = Minecraft.getInstance().hitResult;
    if (result == null || result.getType() != Type.BLOCK) {
      return;
    }
    // must not be broken, must be right interface
    ToolStack tool = ToolStack.from(stack);
    if (tool.isBroken()) {
      return;
    }
    BlockHitResult blockTrace = event.getHitResult();
    BlockPos origin = event.getBlockPos();
    BlockState state = event.getBlockState();
    AOEMatchType matchType = AOEMatchType.BREAKING;
    // if we have any modifier that has an AOE interaction, make our match type more liberal
    if (tool.getModifiers().has(TinkerTags.Modifiers.AOE_INTERACTION)) {
      matchType = AOEMatchType.DISPLAY;
    } else if (!IsEffectiveToolHook.isEffective(tool, state)) {
      return;
    }
    UseOnContext context = new UseOnContext(world, player, InteractionHand.MAIN_HAND, stack, blockTrace);
    Iterator<BlockPos> extraBlocks = tool.getHook(ToolHooks.AOE_ITERATOR).getBlocks(tool, context, state, matchType).iterator();
    if (!extraBlocks.hasNext()) {
      return;
    }

    // Extract immutable shapes now; the render callback must not capture the level.
    List<OutlineData> outlines = new ArrayList<>();
    int rendered = 0;
    do {
      BlockPos pos = extraBlocks.next();
      if (world.getWorldBorder().isWithinBounds(pos)) {
        rendered++;
        BlockState extraState = world.getBlockState(pos);
        outlines.add(new OutlineData(pos.immutable(), extraState.getShape(world, pos, event.getCollisionContext())));
      }
    } while(rendered < MAX_BLOCKS && extraBlocks.hasNext());
    if (!outlines.isEmpty()) {
      event.addCustomRenderer((renderState, buffers, poseStack, translucentPass, levelRenderState) -> {
        if (renderState.isTranslucent() != translucentPass) {
          return false;
        }
        Vec3 camera = levelRenderState.cameraRenderState.pos;
        if (renderState.highContrast()) {
          VertexConsumer contrast = buffers.getBuffer(RenderTypes.secondaryBlockOutline());
          renderOutlines(outlines, poseStack, contrast, camera, -16777216, 7.0F);
        }
        VertexConsumer lines = buffers.getBuffer(RenderTypes.lines());
        int color = renderState.highContrast() ? -11010079 : ARGB.black(102);
        renderOutlines(outlines, poseStack, lines, camera, color, 2.5F);
        return false;
      });
    }
  }

  private static void renderOutlines(List<OutlineData> outlines, PoseStack poseStack, VertexConsumer consumer,
                                     Vec3 camera, int color, float width) {
    for (OutlineData outline : outlines) {
      BlockPos pos = outline.pos();
      ShapeRenderer.renderShape(poseStack, consumer, outline.shape(),
        pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z, color, width);
    }
  }

  /** Renders the block damage process on the extra blocks */
  @SubscribeEvent
  static void renderBlockDamageProgress(ExtractLevelRenderStateEvent event) {
    // validate required variables are set
    MultiPlayerGameMode controller = Minecraft.getInstance().gameMode;
    if (controller == null || !controller.isDestroying()) {
      return;
    }
    Level world = Minecraft.getInstance().level;
    Player player = Minecraft.getInstance().player;
    if (world == null || player == null || Minecraft.getInstance().getCameraEntity() == null) {
      return;
    }
    // must have the right tags
    ItemStack stack = player.getMainHandItem();
    if (stack.isEmpty() || !stack.is(TinkerTags.Items.HARVEST)) {
      return;
    }
    // must be targeting a block
    HitResult result = Minecraft.getInstance().hitResult;
    if (result == null || result.getType() != Type.BLOCK) {
      return;
    }
    // must not be broken, must be right interface
    ToolStack tool = ToolStack.from(stack);
    if (tool.isBroken()) {
      return;
    }
    // find breaking progress
    BlockHitResult blockTrace = (BlockHitResult)result;
    BlockPos target = blockTrace.getBlockPos();
    LevelRenderState renderState = event.getRenderState();
    BlockBreakingRenderState progress = renderState.blockBreakingRenderStates.stream()
      .filter(entry -> entry.blockPos().equals(target))
      .findFirst().orElse(null);
    if (progress == null) {
      return;
    }
    // determine extra blocks to highlight
    BlockState state = world.getBlockState(target);
    // must not be broken, and the tool definition must be effective
    if (!IsEffectiveToolHook.isEffective(tool, state)) {
      return;
    }
    UseOnContext context = new UseOnContext(world, player, InteractionHand.MAIN_HAND, stack, blockTrace.withDirection(BlockSideHitListener.getClientSideHit()));
    Iterator<BlockPos> extraBlocks = tool.getHook(ToolHooks.AOE_ITERATOR).getBlocks(tool, context, state, AreaOfEffectIterator.AOEMatchType.BREAKING).iterator();
    if (!extraBlocks.hasNext()) {
      return;
    }

    int rendered = 0;
    do {
      BlockPos pos = extraBlocks.next();
      if (world.getWorldBorder().isWithinBounds(pos)) {
        renderState.blockBreakingRenderStates.add(
          new BlockBreakingRenderState(pos.immutable(), world.getBlockState(pos), progress.progress()));
        rendered++;
      }
    } while (rendered < MAX_BLOCKS && extraBlocks.hasNext());
  }

  private record OutlineData(BlockPos pos, VoxelShape shape) {}
}
