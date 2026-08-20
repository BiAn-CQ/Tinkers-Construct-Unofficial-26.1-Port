package slimeknights.tconstruct.smeltery.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.BakedQuad.MaterialInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** Applies the casting fade/heat color to deferred 26.1 render submissions. */
public class TintedSubmitNodeCollector implements SubmitNodeCollector {
  private final SubmitNodeCollector delegate;
  private final int tintColor;
  private final int red;
  private final int green;
  private final int blue;
  private final int alpha;

  public TintedSubmitNodeCollector(SubmitNodeCollector delegate, int alpha, int temperature) {
    this.delegate = delegate;
    this.alpha = Math.clamp(alpha, 0, 0xFF);
    temperature = Math.clamp(temperature, 0, 0xFF);
    this.red = 0xFF - temperature * (0xFF - 0xB0) / 0xFF;
    this.green = 0xFF - temperature * (0xFF - 0x60) / 0xFF;
    this.blue = 0xFF - temperature * (0xFF - 0x20) / 0xFF;
    this.tintColor = ARGB.color(this.alpha, this.red, this.green, this.blue);
  }

  @Override
  public OrderedSubmitNodeCollector order(int order) {
    return delegate.order(order);
  }

  @Override
  public void submitShadow(PoseStack pose, float radius, List<EntityRenderState.ShadowPiece> pieces) {
    delegate.submitShadow(pose, radius, pieces);
  }

  @Override
  public void submitNameTag(PoseStack pose, Vec3 offset, int yOffset, Component text, boolean discrete,
                            int light, double distance, CameraRenderState camera) {
    delegate.submitNameTag(pose, offset, yOffset, text, discrete, light, distance, camera);
  }

  @Override
  public void submitText(PoseStack pose, float x, float y, FormattedCharSequence text, boolean dropShadow,
                         Font.DisplayMode mode, int color, int backgroundColor, int light, int outlineColor) {
    delegate.submitText(pose, x, y, text, dropShadow, mode, ARGB.multiply(color, tintColor), backgroundColor, light, outlineColor);
  }

  @Override
  public void submitFlame(PoseStack pose, EntityRenderState state, Quaternionf rotation) {
    delegate.submitFlame(pose, state, rotation);
  }

  @Override
  public void submitLeash(PoseStack pose, EntityRenderState.LeashState state) {
    delegate.submitLeash(pose, state);
  }

  @Override
  public <S> void submitModel(Model<? super S> model, S state, PoseStack pose, RenderType renderType, int light,
                              int overlay, int color, TextureAtlasSprite sprite, int outlineColor,
                              ModelFeatureRenderer.CrumblingOverlay crumbling) {
    delegate.submitModel(model, state, pose, renderType, light, overlay, ARGB.multiply(color, tintColor), sprite, outlineColor, crumbling);
  }

  @Override
  public void submitModelPart(ModelPart part, PoseStack pose, RenderType renderType, int light, int overlay,
                              TextureAtlasSprite sprite, boolean hasFoil, boolean entityGlint, int outlineColor,
                              ModelFeatureRenderer.CrumblingOverlay crumbling, int color) {
    delegate.submitModelPart(part, pose, renderType, light, overlay, sprite, hasFoil, entityGlint, outlineColor, crumbling,
                             ARGB.multiply(color, tintColor));
  }

  @Override
  public void submitMovingBlock(PoseStack pose, MovingBlockRenderState state) {
    delegate.submitMovingBlock(pose, state);
  }

  @Override
  public void submitBlockModel(PoseStack pose, RenderType renderType, List<BlockStateModelPart> parts, int[] tints,
                               int light, int overlay, int outlineColor) {
    int[] colors = tints.clone();
    for (int i = 0; i < colors.length; i++) {
      colors[i] = ARGB.multiply(colors[i], tintColor);
    }
    delegate.submitBlockModel(pose, renderType, parts, colors, light, overlay, outlineColor);
  }

  @Override
  public void submitBreakingBlockModel(PoseStack pose, BlockStateModel model, long seed, int progress) {
    delegate.submitBreakingBlockModel(pose, model, seed, progress);
  }

  @Override
  public void submitItem(PoseStack pose, ItemDisplayContext displayContext, int light, int overlay, int outlineColor,
                         int[] tints, List<BakedQuad> quads, ItemStackRenderState.FoilType foilType) {
    int[] colors = new int[quads.size()];
    List<BakedQuad> tintedQuads = new ArrayList<>(quads.size());
    for (int i = 0; i < quads.size(); i++) {
      BakedQuad quad = quads.get(i);
      MaterialInfo material = quad.materialInfo();
      int baseColor = material.isTinted() && material.tintIndex() >= 0 && material.tintIndex() < tints.length
        ? tints[material.tintIndex()] : -1;
      colors[i] = ARGB.multiply(baseColor, tintColor);
      RenderType renderType = alpha < 0xFF ? RenderTypes.itemTranslucent(material.sprite().atlasLocation()) : material.itemRenderType();
      MaterialInfo tintedMaterial = new MaterialInfo(material.sprite(), material.layer(), renderType, i,
                                                       material.shade(), material.lightEmission());
      tintedQuads.add(new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
                                    quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(),
                                    quad.direction(), tintedMaterial));
    }
    delegate.submitItem(pose, displayContext, light, overlay, outlineColor, colors, tintedQuads, foilType);
  }

  @Override
  public void submitCustomGeometry(PoseStack pose, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer renderer) {
    delegate.submitCustomGeometry(pose, renderType,
      (immutablePose, buffer) -> renderer.render(immutablePose, new TintedVertexBuilder(buffer, red, green, blue, alpha)));
  }

  @Override
  public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer renderer) {
    delegate.submitParticleGroup(renderer);
  }
}
