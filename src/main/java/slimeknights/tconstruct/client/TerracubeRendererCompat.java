package slimeknights.tconstruct.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.slime.MagmaCubeModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.world.entity.TerracubeEntity;

/** Native 26.1 renderer for the clay terracube model and squish animation. */
public class TerracubeRendererCompat extends MobRenderer<TerracubeEntity, SlimeRenderState, MagmaCubeModel> {
  private static final Identifier TEXTURE = TConstruct.getResource("textures/entity/terracube.png");
  static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(TConstruct.getResource("terracube"), "main");

  public TerracubeRendererCompat(EntityRendererProvider.Context context) {
    super(context, new MagmaCubeModel(context.bakeLayer(MODEL_LAYER)), 0.25f);
    addLayer(new SlimeHeadLayerCompat<>(this, context, true));
  }

  /**
   * Keeps the 64x32 UV contract used by the Tinkers terracube texture.
   * Minecraft 26.1 changed the vanilla magma-cube layer to a 64x64 layout,
   * which otherwise samples unrelated/empty pixels from the legacy texture.
   */
  static LayerDefinition createBodyLayer() {
    MeshDefinition mesh = new MeshDefinition();
    PartDefinition root = mesh.getRoot();
    for (int i = 0; i < 8; i++) {
      int u = 0;
      int v = i;
      if (i == 2) {
        u = 24;
        v = 10;
      } else if (i == 3) {
        u = 24;
        v = 19;
      }
      root.addOrReplaceChild("cube" + i,
        CubeListBuilder.create().texOffs(u, v).addBox(-4.0f, 16.0f + i, -4.0f, 8.0f, 1.0f, 8.0f),
        PartPose.ZERO);
    }
    root.addOrReplaceChild("inside_cube",
      CubeListBuilder.create().texOffs(0, 16).addBox(-2.0f, 18.0f, -2.0f, 4.0f, 4.0f, 4.0f),
      PartPose.ZERO);
    return LayerDefinition.create(mesh, 64, 32);
  }

  @Override
  public Identifier getTextureLocation(SlimeRenderState state) {
    return TEXTURE;
  }

  @Override
  public SlimeRenderState createRenderState() {
    return new SlimeEquipmentRenderState();
  }

  @Override
  public void extractRenderState(TerracubeEntity entity, SlimeRenderState state, float partialTicks) {
    super.extractRenderState(entity, state, partialTicks);
    state.squish = Mth.lerp(partialTicks, entity.oSquish, entity.squish);
    state.size = entity.getSize();
    if (state instanceof SlimeEquipmentRenderState equipmentState) {
      HumanoidMobRenderer.extractHumanoidRenderState(entity, equipmentState.armorState, partialTicks, itemModelResolver);
    }
  }

  @Override
  protected float getShadowRadius(SlimeRenderState state) {
    return state.size * 0.25f;
  }

  @Override
  protected void scale(SlimeRenderState state, PoseStack poseStack) {
    int size = state.size;
    float squish = state.squish / (size * 0.5f + 1.0f);
    float inverse = 1.0f / (squish + 1.0f);
    poseStack.scale(inverse * size, size / inverse, inverse * size);
  }
}
