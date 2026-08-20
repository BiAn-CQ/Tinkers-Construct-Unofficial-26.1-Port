package slimeknights.tconstruct.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import java.util.function.Function;

/** Translucent slime shell which follows the owning renderer's texture selection. */
final class TinkerSlimeOuterLayerCompat extends RenderLayer<SlimeRenderState,SlimeModel> {
  private final SlimeModel model;
  private final Function<SlimeRenderState,Identifier> textureSelector;

  TinkerSlimeOuterLayerCompat(RenderLayerParent<SlimeRenderState,SlimeModel> parent, EntityModelSet modelSet,
                             Function<SlimeRenderState,Identifier> textureSelector) {
    super(parent);
    this.model = new SlimeModel(modelSet.bakeLayer(ModelLayers.SLIME_OUTER));
    this.textureSelector = textureSelector;
  }

  @Override
  public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                     SlimeRenderState state, float yRot, float xRot) {
    boolean glowingInvisible = state.appearsGlowing() && state.isInvisible;
    if (state.isInvisible && !glowingInvisible) {
      return;
    }

    Identifier texture = textureSelector.apply(state);
    int overlayCoords = LivingEntityRenderer.getOverlayCoords(state, 0.0f);
    submitNodeCollector.order(1).submitModel(
      model,
      state,
      poseStack,
      glowingInvisible ? RenderTypes.outline(texture) : RenderTypes.entityTranslucent(texture),
      lightCoords,
      overlayCoords,
      state.outlineColor,
      null);
  }
}
