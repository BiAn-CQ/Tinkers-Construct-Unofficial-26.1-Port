package slimeknights.tconstruct.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.level.block.SkullBlock;

import java.util.function.Function;

/** Renders the helmet, skull, or ordinary head item carried by an armored slime. */
final class SlimeHeadLayerCompat<M extends EntityModel<? super SlimeRenderState>> extends RenderLayer<SlimeRenderState, M> {
  private final HumanoidModel<HumanoidRenderState> armorModel;
  private final EquipmentLayerRenderer equipmentRenderer;
  private final Function<SkullBlock.Type, SkullModelBase> skullModels;
  private final PlayerSkinRenderCache playerSkinRenderCache;
  private final boolean lavaSlime;

  SlimeHeadLayerCompat(RenderLayerParent<SlimeRenderState, M> parent, EntityRendererProvider.Context context, boolean lavaSlime) {
    super(parent);
    this.armorModel = new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_ARMOR.head()));
    this.armorModel.head.visible = true;
    this.armorModel.hat.visible = true;
    this.equipmentRenderer = context.getEquipmentRenderer();
    this.skullModels = Util.memoize(type -> SkullBlockRenderer.createModel(context.getModelSet(), type));
    this.playerSkinRenderCache = context.getPlayerSkinRenderCache();
    this.lavaSlime = lavaSlime;
  }

  @Override
  public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                     SlimeRenderState state, float yRot, float xRot) {
    if (!(state instanceof SlimeEquipmentRenderState equipmentState)) {
      return;
    }
    ItemStack helmet = equipmentState.armorState.headEquipment;
    if (helmet.isEmpty() && state.headItem.isEmpty() && state.wornHeadType == null) {
      return;
    }

    poseStack.pushPose();
    if (lavaSlime) {
      poseStack.translate(0, 1.5 - 0.425 * Math.max(0, state.squish), 0);
    } else {
      poseStack.translate(0, 1.5, 0);
    }
    poseStack.scale(0.9f, 0.9f, 0.9f);

    if (!helmet.isEmpty()) {
      Equippable equippable = helmet.get(DataComponents.EQUIPPABLE);
      if (equippable != null && equippable.assetId().isPresent()) {
        equipmentRenderer.renderLayers(
          EquipmentClientInfo.LayerType.HUMANOID,
          equippable.assetId().orElseThrow(),
          armorModel,
          equipmentState.armorState,
          helmet,
          poseStack,
          submitNodeCollector,
          lightCoords,
          state.outlineColor);
      }
    } else if (state.wornHeadType != null) {
      poseStack.scale(1.1875f, -1.1875f, -1.1875f);
      poseStack.translate(-0.5, 0, -0.5);
      SkullBlock.Type type = state.wornHeadType;
      SkullBlockRenderer.submitSkull(
        state.wornHeadAnimationPos,
        poseStack,
        submitNodeCollector,
        lightCoords,
        skullModels.apply(type),
        resolveSkullRenderType(state, type),
        state.outlineColor,
        null);
    } else {
      CustomHeadLayer.translateToHead(poseStack, CustomHeadLayer.Transforms.DEFAULT);
      state.headItem.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
    }
    poseStack.popPose();
  }

  private RenderType resolveSkullRenderType(SlimeRenderState state, SkullBlock.Type type) {
    if (type == SkullBlock.Types.PLAYER) {
      ResolvableProfile profile = state.wornHeadProfile;
      if (profile != null) {
        return playerSkinRenderCache.getOrDefault(profile).renderType();
      }
    }
    return SkullBlockRenderer.getSkullRenderType(type, null);
  }
}
