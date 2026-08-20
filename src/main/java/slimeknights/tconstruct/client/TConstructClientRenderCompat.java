package slimeknights.tconstruct.client;

import net.minecraft.client.model.object.skull.PiglinHeadModel;
import net.minecraft.client.model.object.skull.SkullModel;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.CreateSkullModels;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.gadgets.TinkerGadgets;
import slimeknights.tconstruct.tools.client.material.ThrownShurikenRenderer;
import slimeknights.tconstruct.world.TinkerHeadType;
import slimeknights.tconstruct.world.TinkerWorld;

import java.util.EnumMap;
import java.util.Map;

/**
 * Small native 26.1 client bridge for the parts of the legacy client event
 * tree that are required before the larger renderer migration is complete.
 *
 * <p>In particular, 26.1 no longer accepts the old item-model parent-only
 * skull definitions. The native head special model uses the skull renderer
 * maps, so custom Tinker skull types and textures must be available during
 * the initial model bake.</p>
 */
@EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT)
public final class TConstructClientRenderCompat {
  private static final Map<TinkerHeadType,ModelLayerLocation> HEAD_LAYERS = new EnumMap<>(TinkerHeadType.class);

  private TConstructClientRenderCompat() {}

  static {
    registerSkullTexture(TinkerHeadType.BLAZE, Identifier.parse("textures/entity/blaze/blaze.png"));
    registerSkullTexture(TinkerHeadType.ENDERMAN, TConstruct.getResource("textures/entity/skull/enderman.png"));
    registerSkullTexture(TinkerHeadType.STRAY, TConstruct.getResource("textures/entity/skull/stray.png"));
    registerSkullTexture(TinkerHeadType.HUSK, Identifier.parse("textures/entity/zombie/husk.png"));
    registerSkullTexture(TinkerHeadType.DROWNED, TConstruct.getResource("textures/entity/skull/drowned.png"));
    registerSkullTexture(TinkerHeadType.SPIDER, Identifier.parse("textures/entity/spider/spider.png"));
    registerSkullTexture(TinkerHeadType.CAVE_SPIDER, Identifier.parse("textures/entity/spider/cave_spider.png"));
    registerSkullTexture(TinkerHeadType.PIGLIN_BRUTE, Identifier.parse("textures/entity/piglin/piglin_brute.png"));
    registerSkullTexture(TinkerHeadType.ZOMBIFIED_PIGLIN, Identifier.parse("textures/entity/piglin/zombified_piglin.png"));
    registerSkullTexture(TinkerHeadType.VENOMBONE, TConstruct.getResource("textures/entity/skull/venombone.png"));
    registerSkullTexture(TinkerHeadType.BLAZING_BONE, TConstruct.getResource("textures/entity/skull/blazing_bone.png"));
    registerSkullTexture(TinkerHeadType.NECRONIUM, TConstruct.getResource("textures/entity/skull/necronium.png"));
    for (TinkerHeadType type : TinkerHeadType.values()) {
      HEAD_LAYERS.put(type, new ModelLayerLocation(TConstruct.getResource(type.getSerializedName() + "_head"), "main"));
    }
  }

  private static void registerSkullTexture(TinkerHeadType type, Identifier texture) {
    SkullBlockRenderer.SKIN_BY_TYPE.put(type, texture);
  }

  /** Restores the per-mob head geometry used by the 1.20.1 client. */
  @SubscribeEvent
  static void registerSkullLayerDefinitions(RegisterLayerDefinitions event) {
    for (TinkerHeadType type : TinkerHeadType.values()) {
      event.registerLayerDefinition(HEAD_LAYERS.get(type), () -> createHeadLayer(type));
    }
    event.registerLayerDefinition(TerracubeRendererCompat.MODEL_LAYER, TerracubeRendererCompat::createBodyLayer);
  }

  @SubscribeEvent
  static void registerSkullModels(CreateSkullModels event) {
    for (TinkerHeadType type : TinkerHeadType.values()) {
      if (type.isPiglin()) {
        event.registerSkullModel(type,
          modelSet -> new PiglinHeadModel(modelSet.bakeLayer(HEAD_LAYERS.get(type))),
          null);
      } else {
        event.registerSkullModel(type,
          modelSet -> new SkullModel(modelSet.bakeLayer(HEAD_LAYERS.get(type))),
          null);
      }
    }
  }

  private static LayerDefinition createHeadLayer(TinkerHeadType type) {
    return switch (type) {
      case BLAZE -> SkullModel.createMobHeadLayer();
      case ENDERMAN, VENOMBONE, BLAZING_BONE, NECRONIUM -> createHeadLayer(0, 0, 32, 16);
      case STRAY, DROWNED -> createHeadHatLayer(0, 16, 32, 32);
      case HUSK -> createHeadLayer(0, 0, 64, 64);
      case SPIDER, CAVE_SPIDER -> createHeadLayer(32, 4, 64, 32);
      case PIGLIN_BRUTE, ZOMBIFIED_PIGLIN -> LayerDefinition.create(PiglinHeadModel.createHeadModel(), 64, 64);
    };
  }

  private static LayerDefinition createHeadLayer(int textureX, int textureY, int textureWidth, int textureHeight) {
    MeshDefinition mesh = new MeshDefinition();
    mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create()
      .texOffs(textureX, textureY)
      .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
    return LayerDefinition.create(mesh, textureWidth, textureHeight);
  }

  private static LayerDefinition createHeadHatLayer(int hatX, int hatY, int textureWidth, int textureHeight) {
    MeshDefinition mesh = SkullModel.createHeadModel();
    mesh.getRoot().getChild("head").addOrReplaceChild("hat", CubeListBuilder.create()
      .texOffs(hatX, hatY)
      .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.25F)), PartPose.ZERO);
    return LayerDefinition.create(mesh, textureWidth, textureHeight);
  }

  @SubscribeEvent
  static void registerStandaloneModels(ModelEvent.RegisterStandalone event) {
    FancyItemFrameRendererCompat.registerModels(event);
  }

  @SubscribeEvent
  static void registerEntityRenderers(RegisterRenderers event) {
    event.registerEntityRenderer(TinkerWorld.skySlimeEntity.get(), context -> new TinkerSlimeRendererCompat(
      context,
      TConstruct.getResource("textures/entity/sky_slime.png"),
      TConstruct.getResource("textures/entity/steel_slime.png")));
    event.registerEntityRenderer(TinkerWorld.enderSlimeEntity.get(), context -> new TinkerSlimeRendererCompat(
      context,
      TConstruct.getResource("textures/entity/ender_slime.png"),
      TConstruct.getResource("textures/entity/knightmetal_slime.png")));
    event.registerEntityRenderer(TinkerWorld.terracubeEntity.get(), TerracubeRendererCompat::new);

    event.registerEntityRenderer(TinkerGadgets.itemFrameEntity.get(), FancyItemFrameRendererCompat::new);
    event.registerEntityRenderer(TinkerGadgets.glowBallEntity.get(), ThrownItemRenderer::new);
    event.registerEntityRenderer(TinkerGadgets.eflnEntity.get(), ThrownItemRenderer::new);
    event.registerEntityRenderer(TinkerGadgets.quartzShurikenEntity.get(), ThrownShurikenRenderer::new);
    event.registerEntityRenderer(TinkerGadgets.flintShurikenEntity.get(), ThrownShurikenRenderer::new);
  }
}
