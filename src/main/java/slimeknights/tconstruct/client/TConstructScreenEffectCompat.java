package slimeknights.tconstruct.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;

/** Native 26.1 first-person overlay for transparent TConstruct blocks. */
@EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT)
public final class TConstructScreenEffectCompat {
  private TConstructScreenEffectCompat() {}

  @SubscribeEvent
  static void renderTransparentBlockOverlay(RenderBlockScreenEffectEvent event) {
    BlockState state = event.getBlockState();
    if (event.getOverlayType() != RenderBlockScreenEffectEvent.OverlayType.BLOCK
        || !state.is(TinkerTags.Blocks.TRANSPARENT_OVERLAY)) {
      return;
    }

    Player player = event.getPlayer();
    BlockPos pos = event.getBlockPos();
    float width = player.getBbWidth() * 0.8f;
    if (Shapes.joinIsNotEmpty(
      state.getShape(player.level(), pos).move(pos.getX(), pos.getY(), pos.getZ()),
      Shapes.create(AABB.ofSize(player.getEyePosition(), width, 1.0E-6d, width)), BooleanOp.AND)) {
      Minecraft minecraft = Minecraft.getInstance();
      TextureAtlasSprite sprite = minecraft.getModelManager().getBlockStateModelSet().getParticleMaterial(state).sprite();
      BlockPos eyePos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
      float brightness = Lightmap.getBrightness(player.level().dimensionType(), player.level().getMaxLocalRawBrightness(eyePos));
      int color = ARGB.white(brightness);
      PoseStack.Pose pose = event.getPoseStack().last();
      VertexConsumer buffer = event.getBufferSource().getBuffer(RenderTypes.blockScreenEffect(sprite.atlasLocation()));
      buffer.addVertex(pose, -1, -1, -0.5f).setUv(sprite.getU1(), sprite.getV1()).setColor(color);
      buffer.addVertex(pose,  1, -1, -0.5f).setUv(sprite.getU0(), sprite.getV1()).setColor(color);
      buffer.addVertex(pose,  1,  1, -0.5f).setUv(sprite.getU0(), sprite.getV0()).setColor(color);
      buffer.addVertex(pose, -1,  1, -0.5f).setUv(sprite.getU1(), sprite.getV0()).setColor(color);
    }
    event.setCanceled(true);
  }
}
