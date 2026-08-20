package slimeknights.tconstruct.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SkullBlock;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.compat.ArmorItem;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.nbt.MaterialIdNBT;
import slimeknights.tconstruct.tools.TinkerTools;
import slimeknights.tconstruct.tools.data.material.MaterialIds;
import slimeknights.tconstruct.world.TinkerHeadType;

import javax.annotation.Nullable;
import java.util.function.Function;

/** Renders the mob head selected by the first material of a worn slimeskull. */
final class SlimeskullHeadLayerCompat extends RenderLayer<AvatarRenderState,PlayerModel> {
  private static final Identifier FLUID_CANNON_TEXTURE = TConstruct.getResource("textures/entity/skull/fluid_cannon.png");

  private final Function<SkullBlock.Type,SkullModelBase> skullModels;

  SlimeskullHeadLayerCompat(RenderLayerParent<AvatarRenderState,PlayerModel> parent, EntityModelSet entityModels) {
    super(parent);
    this.skullModels = Util.memoize(type -> SkullBlockRenderer.createModel(entityModels, type));
  }

  @Override
  public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                     AvatarRenderState state, float yRot, float xRot) {
    ItemStack stack = state.headEquipment;
    if (!stack.is(TinkerTools.slimesuit.get(ArmorItem.Type.HELMET))) {
      return;
    }

    MaterialIdNBT materials = MaterialIdNBT.from(stack);
    HeadStyle style = getHeadStyle(materials.getMaterial(0).getId());
    if (style == null) {
      return;
    }

    SkullModelBase model = skullModels.apply(style.type());
    if (model == null) {
      return;
    }
    RenderType renderType = SkullBlockRenderer.getSkullRenderType(style.type(), style.texture());

    SkullModelBase.State skullState = new SkullModelBase.State();
    skullState.animationPos = state.wornHeadAnimationPos;

    poseStack.pushPose();
    getParentModel().root().translateAndRotate(poseStack);
    getParentModel().translateToHead(poseStack);
    // Match the legacy slimeskull fit instead of the larger vanilla worn-skull scale.
    poseStack.scale(1.115F, 1.115F, 1.115F);
    collector.submitModel(
      model,
      skullState,
      poseStack,
      renderType,
      lightCoords,
      OverlayTexture.NO_OVERLAY,
      getHeadColor(materials.getMaterial(1)),
      null,
      state.outlineColor,
      null
    );
    poseStack.popPose();
  }

  private static int getHeadColor(MaterialVariantId material) {
    if (IMaterial.UNKNOWN_ID.equals(material)) {
      return -1;
    }
    return MaterialRenderInfoLoader.INSTANCE.getRenderInfo(material)
      .map(MaterialRenderInfo::vertexColor)
      .orElse(-1);
  }

  @Nullable
  private static HeadStyle getHeadStyle(MaterialId material) {
    if (material.equals(MaterialIds.glass))         return vanilla(SkullBlock.Types.CREEPER);
    if (material.equals(MaterialIds.dragonScale))  return vanilla(SkullBlock.Types.DRAGON);
    if (material.equals(MaterialIds.leather))      return vanilla(SkullBlock.Types.ZOMBIE);
    if (material.equals(MaterialIds.bone))         return vanilla(SkullBlock.Types.SKELETON);
    if (material.equals(MaterialIds.necroticBone)) return vanilla(SkullBlock.Types.WITHER_SKELETON);
    if (material.equals(MaterialIds.gold))         return vanilla(SkullBlock.Types.PIGLIN);

    if (material.equals(MaterialIds.blaze))        return tinker(TinkerHeadType.BLAZE);
    if (material.equals(MaterialIds.enderPearl))   return tinker(TinkerHeadType.ENDERMAN);
    if (material.equals(MaterialIds.ice))          return tinker(TinkerHeadType.STRAY);
    if (material.equals(MaterialIds.iron))         return tinker(TinkerHeadType.HUSK);
    if (material.equals(MaterialIds.copper))       return tinker(TinkerHeadType.DROWNED);
    if (material.equals(MaterialIds.string))       return tinker(TinkerHeadType.SPIDER);
    if (material.equals(MaterialIds.darkthread))   return tinker(TinkerHeadType.CAVE_SPIDER);
    if (material.equals(MaterialIds.roseGold))     return tinker(TinkerHeadType.PIGLIN_BRUTE);
    if (material.equals(MaterialIds.pigIron))      return tinker(TinkerHeadType.ZOMBIFIED_PIGLIN);
    if (material.equals(MaterialIds.venombone))    return tinker(TinkerHeadType.VENOMBONE);
    if (material.equals(MaterialIds.blazingBone)) return tinker(TinkerHeadType.BLAZING_BONE);
    if (material.equals(MaterialIds.necronium))    return tinker(TinkerHeadType.NECRONIUM);
    if (material.equals(MaterialIds.knightmetal)) return new HeadStyle(TinkerHeadType.DROWNED, FLUID_CANNON_TEXTURE);
    return null;
  }

  private static HeadStyle vanilla(SkullBlock.Types type) {
    return new HeadStyle(type, null);
  }

  private static HeadStyle tinker(TinkerHeadType type) {
    return new HeadStyle(type, null);
  }

  private record HeadStyle(SkullBlock.Type type, @Nullable Identifier texture) {}
}
