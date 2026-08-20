package slimeknights.tconstruct.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.List;

/**
 * Restores Tinkers' material luminosity on top of the native 26.1 equipment
 * renderer.
 *
 * <p>The native renderer owns the equipment asset and accepts texture/tint
 * overrides, but it always uses the entity light passed by the vanilla layer.
 * Tinkers' armor materials also carry an independent light level, so glowing
 * material layers need one additional full-bright submission.  This layer is
 * deliberately limited to the four Tinkers equipment assets and only submits
 * layers whose selected material is emissive.</p>
 */
final class TConstructArmorGlowLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
  private static final int MAX_LIGHT = LightmapLight.pack(15, 15);
  private static final Identifier ARMOR_ROOT = Identifier.fromNamespaceAndPath("tconstruct", "tinker_armor");

  private final ArmorModelSet<PlayerModel> armorModels;
  private final ElytraModel wingsModel;

  TConstructArmorGlowLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent,
                           net.minecraft.client.model.geom.EntityModelSet entityModels,
                           boolean slim) {
    super(parent);
    this.armorModels = ArmorModelSet.bake(
      slim ? ModelLayers.PLAYER_SLIM_ARMOR : ModelLayers.PLAYER_ARMOR,
      entityModels,
      part -> new PlayerModel(part, slim)
    );
    this.wingsModel = new ElytraModel(entityModels.bakeLayer(ModelLayers.ELYTRA));
  }

  @Override
  public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                     AvatarRenderState state, float yRot, float xRot) {
    int order = 0;
    order = renderArmor(poseStack, submitNodeCollector, state, state.chestEquipment,
      EquipmentSlot.CHEST, armorLayerType(state, EquipmentSlot.CHEST), armorModels.get(EquipmentSlot.CHEST), lightCoords, order);
    order = renderArmor(poseStack, submitNodeCollector, state, state.legsEquipment,
      EquipmentSlot.LEGS, armorLayerType(state, EquipmentSlot.LEGS), armorModels.get(EquipmentSlot.LEGS), lightCoords, order);
    order = renderArmor(poseStack, submitNodeCollector, state, state.feetEquipment,
      EquipmentSlot.FEET, armorLayerType(state, EquipmentSlot.FEET), armorModels.get(EquipmentSlot.FEET), lightCoords, order);
    order = renderArmor(poseStack, submitNodeCollector, state, state.headEquipment,
      EquipmentSlot.HEAD, armorLayerType(state, EquipmentSlot.HEAD), armorModels.get(EquipmentSlot.HEAD), lightCoords, order);
    renderWings(poseStack, submitNodeCollector, state, state.chestEquipment, lightCoords, order);
  }

  private int renderArmor(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state,
                          ItemStack stack, EquipmentSlot slot, EquipmentClientInfo.LayerType layerType,
                          PlayerModel model, int lightCoords, int order) {
    Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
    if (equippable == null || equippable.slot() != slot || equippable.assetId().isEmpty()) {
      return order;
    }
    if (!(IClientItemExtensions.of(stack) instanceof TConstructArmorClientExtensions extension)) {
      return order;
    }

    List<EquipmentClientInfo.Layer> layers = layers(equippable.assetId().orElseThrow(), layerType);
    int fallbackColor = extension.getDefaultDyeColor(stack);
    for (int layerIdx = 0; layerIdx < layers.size(); layerIdx++) {
      EquipmentClientInfo.Layer layer = layers.get(layerIdx);
      int luminosity = extension.getArmorLuminosity(stack, layer.textureId().getPath());
      if (luminosity <= 0) {
        continue;
      }
      int color = extension.getArmorLayerTintColor(stack, layer, layerIdx, fallbackColor);
      if (color == 0) {
        continue;
      }
      Identifier texture = ClientHooks.getArmorTexture(stack, layerType, layer, layer.getTextureLocation(layerType));
      collector.order(order++).submitModel(
        model,
        state,
        poseStack,
        RenderTypes.armorCutoutNoCull(texture),
        applyLuminosity(lightCoords, luminosity),
        OverlayTexture.NO_OVERLAY,
        color,
        null,
        state.outlineColor,
        null
      );
    }
    return order;
  }

  private int renderWings(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state,
                          ItemStack stack, int lightCoords, int order) {
    Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
    if (equippable == null || equippable.slot() != EquipmentSlot.CHEST || equippable.assetId().isEmpty()) {
      return order;
    }
    if (!(IClientItemExtensions.of(stack) instanceof TConstructArmorClientExtensions extension)) {
      return order;
    }

    EquipmentClientInfo.Layer layer = new EquipmentClientInfo.Layer(texture("slime/wings"));
    int luminosity = extension.getArmorLuminosity(stack, layer.textureId().getPath());
    if (luminosity <= 0) {
      return order;
    }
    int color = extension.getArmorLayerTintColor(stack, layer, 0, extension.getDefaultDyeColor(stack));
    if (color == 0) {
      return order;
    }

    Identifier texture = ClientHooks.getArmorTexture(
      stack,
      EquipmentClientInfo.LayerType.WINGS,
      layer,
      layer.getTextureLocation(EquipmentClientInfo.LayerType.WINGS)
    );
    poseStack.pushPose();
    poseStack.translate(0.0F, 0.0F, 0.125F);
    collector.order(order).submitModel(
      wingsModel,
      state,
      poseStack,
      RenderTypes.armorCutoutNoCull(texture),
      applyLuminosity(lightCoords, luminosity),
      OverlayTexture.NO_OVERLAY,
      color,
      null,
      state.outlineColor,
      null
    );
    poseStack.popPose();
    return order + 1;
  }

  private static List<EquipmentClientInfo.Layer> layers(ResourceKey<EquipmentAsset> asset,
                                                         EquipmentClientInfo.LayerType layerType) {
    String path = asset.identifier().getPath();
    if ("travelers".equals(path)) {
      return layerType == EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS
        ? List.of(layer("travelers/base_leggings"), layer("travelers/cuirass_leggings"), layer("travelers/metal_leggings"))
        : List.of(layer("travelers/base_armor"), layer("travelers/cuirass_armor"), layer("travelers/metal_armor"));
    }
    if ("plate".equals(path)) {
      return layerType == EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS
        ? List.of(layer("plate/plating_leggings"), layer("plate/maille_leggings"))
        : List.of(layer("plate/plating_armor"), layer("plate/maille_armor"));
    }
    if ("slime".equals(path)) {
      return List.of(layer(layerType == EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS ? "slime/leggings" : "slime/armor"));
    }
    return List.of();
  }

  private static EquipmentClientInfo.LayerType armorLayerType(AvatarRenderState state, EquipmentSlot slot) {
    return state.isBaby
      ? EquipmentClientInfo.LayerType.HUMANOID_BABY
      : slot == EquipmentSlot.LEGS
        ? EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS
        : EquipmentClientInfo.LayerType.HUMANOID;
  }

  private static EquipmentClientInfo.Layer layer(String path) {
    return new EquipmentClientInfo.Layer(texture(path));
  }

  private static Identifier texture(String path) {
    return ARMOR_ROOT.withPath(ARMOR_ROOT.getPath() + "/" + path);
  }

  private static int applyLuminosity(int packedLight, int luminosity) {
    if (luminosity >= 15) {
      return MAX_LIGHT;
    }
    int block = Math.max(luminosity, (packedLight & 0xFFFF) >> 4) << 4;
    int sky = Math.max(luminosity, (packedLight >> 20) & 0xFFFF) << 20;
    return block | sky;
  }

  /** Avoids loading the legacy LightTexture class solely for a constant. */
  private static final class LightmapLight {
    private static int pack(int block, int sky) {
      return (block << 4) | (sky << 20);
    }
  }
}
