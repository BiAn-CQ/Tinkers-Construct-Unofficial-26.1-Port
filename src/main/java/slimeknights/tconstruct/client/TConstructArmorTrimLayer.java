package slimeknights.tconstruct.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.modules.cosmetic.TrimModule;

import javax.annotation.Nullable;

/**
 * Renders Tinkers' persistent armor trims through the native 26.1 armor-trim
 * atlas.  Tinkers stores its trim as modifier data rather than the vanilla
 * {@code DataComponents.TRIM} component, so the vanilla EquipmentLayerRenderer
 * cannot see it on its own.
 */
final class TConstructArmorTrimLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
  private final ArmorModelSet<PlayerModel> modelSet;
  private final TextureAtlas trimAtlas;

  TConstructArmorTrimLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent,
                           net.minecraft.client.model.geom.EntityModelSet entityModels,
                           boolean slim) {
    super(parent);
    this.modelSet = ArmorModelSet.bake(
      slim ? ModelLayers.PLAYER_SLIM_ARMOR : ModelLayers.PLAYER_ARMOR,
      entityModels,
      part -> new PlayerModel(part, slim)
    );
    this.trimAtlas = Minecraft.getInstance().getAtlasManager()
      .getAtlasOrThrow(AtlasIds.ARMOR_TRIMS);
  }

  @Override
  public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                     AvatarRenderState state, float yRot, float xRot) {
    int order = 0;
    order = renderTrim(poseStack, submitNodeCollector, lightCoords, state,
      state.chestEquipment, EquipmentSlot.CHEST, order);
    order = renderTrim(poseStack, submitNodeCollector, lightCoords, state,
      state.legsEquipment, EquipmentSlot.LEGS, order);
    order = renderTrim(poseStack, submitNodeCollector, lightCoords, state,
      state.feetEquipment, EquipmentSlot.FEET, order);
    renderTrim(poseStack, submitNodeCollector, lightCoords, state,
      state.headEquipment, EquipmentSlot.HEAD, order);
  }

  private int renderTrim(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                         AvatarRenderState state, ItemStack stack, EquipmentSlot slot, int order) {
    ArmorTrim trim = getTrim(stack);
    if (trim == null) {
      return order;
    }

    Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
    if (equippable == null || equippable.slot() != slot || equippable.assetId().isEmpty()) {
      return order;
    }

    EquipmentClientInfo.LayerType layerType = state.isBaby
      ? EquipmentClientInfo.LayerType.HUMANOID_BABY
      : slot == EquipmentSlot.LEGS
        ? EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS
        : EquipmentClientInfo.LayerType.HUMANOID;
    Identifier spriteId = trim.layerAssetId(layerType.trimAssetPrefix(), equippable.assetId().orElseThrow());
    TextureAtlasSprite sprite = trimAtlas.getSprite(spriteId);
    if (MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name())) {
      return order;
    }

    PlayerModel model = modelSet.get(slot);
    submitNodeCollector.order(order).submitModel(
      model,
      state,
      poseStack,
      Sheets.armorTrimsSheet(trim.pattern().value().decal()),
      lightCoords,
      OverlayTexture.NO_OVERLAY,
      -1,
      sprite,
      state.outlineColor,
      null
    );
    return order + 1;
  }

  @Nullable
  private static ArmorTrim getTrim(ItemStack stack) {
    if (stack.isEmpty() || ModifierUtil.getModifierLevel(stack, TinkerModifiers.trim.getId()) <= 0) {
      return null;
    }

    String materialId = ModifierUtil.getPersistentString(stack, TrimModule.materialKey(TinkerModifiers.trim.getId()));
    String patternId = ModifierUtil.getPersistentString(stack, TrimModule.patternKey(TinkerModifiers.trim.getId()));
    if (materialId.isEmpty() || patternId.isEmpty()) {
      return null;
    }

    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.level == null) {
      return null;
    }
    Identifier materialKey = Identifier.tryParse(materialId);
    Identifier patternKey = Identifier.tryParse(patternId);
    if (materialKey == null || patternKey == null) {
      return null;
    }

    RegistryAccess access = minecraft.level.registryAccess();
    Holder<TrimMaterial> material = access.lookupOrThrow(Registries.TRIM_MATERIAL)
      .get(materialKey).orElse(null);
    Holder<TrimPattern> pattern = access.lookupOrThrow(Registries.TRIM_PATTERN)
      .get(patternKey).orElse(null);
    return material == null || pattern == null ? null : new ArmorTrim(material, pattern);
  }
}
