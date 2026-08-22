package slimeknights.tconstruct.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import slimeknights.tconstruct.client.TConstructArmorClientExtensions;
import slimeknights.tconstruct.client.SlimeskullRenderer;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Adds Tinkers' material luminosity to the native equipment renderer.
 *
 * <p>Doing this at the equipment renderer keeps the exact armor model chosen by
 * vanilla, including armor stands, babies, zombies, and modded humanoids. It
 * also avoids entity-type branches in Tinkers' client compatibility code.</p>
 */
@Mixin(EquipmentLayerRenderer.class)
public abstract class EquipmentLayerRendererMixin {
  private static final int EMISSIVE_ORDER_OFFSET = 16;
  private static final int MAX_LIGHT = (15 << 4) | (15 << 20);

  @Shadow @Final
  private EquipmentAssetManager equipmentAssets;

  @Inject(
    method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
    at = @At("TAIL")
  )
  private <S> void tconstruct$renderMaterialLuminosity(
    EquipmentClientInfo.LayerType layerType,
    ResourceKey<EquipmentAsset> asset,
    Model<? super S> model,
    S state,
    ItemStack stack,
    PoseStack poseStack,
    SubmitNodeCollector collector,
    int packedLight,
    @Nullable Identifier textureOverride,
    int outlineColor,
    int startingOrder,
    CallbackInfo callback
  ) {
    if (!(IClientItemExtensions.of(stack) instanceof TConstructArmorClientExtensions extension)) {
      return;
    }

    List<EquipmentClientInfo.Layer> layers = equipmentAssets.get(asset).getLayers(layerType);
    if (layers.isEmpty()) {
      return;
    }

    int order = startingOrder + EMISSIVE_ORDER_OFFSET;
    int fallbackColor = extension.getDefaultDyeColor(stack);
    for (int layerIndex = 0; layerIndex < layers.size(); layerIndex++) {
      EquipmentClientInfo.Layer layer = layers.get(layerIndex);
      int luminosity = extension.getArmorLuminosity(stack, layer.textureId().getPath());
      if (luminosity <= 0) {
        continue;
      }
      int color = extension.getArmorLayerTintColor(stack, layer, layerIndex, fallbackColor);
      if (color == 0) {
        continue;
      }
      Identifier texture = layer.usePlayerTexture() && textureOverride != null
        ? textureOverride
        : ClientHooks.getArmorTexture(stack, layerType, layer, layer.getTextureLocation(layerType));
      collector.order(order++).submitModel(
        model,
        state,
        poseStack,
        RenderTypes.armorCutoutNoCull(texture),
        applyLuminosity(packedLight, luminosity),
        OverlayTexture.NO_OVERLAY,
        color,
        null,
        outlineColor,
        null
      );
    }

    SlimeskullRenderer.submit(
      model, state, stack, poseStack, collector.order(order), packedLight, outlineColor
    );
  }

  private static int applyLuminosity(int packedLight, int luminosity) {
    if (luminosity >= 15) {
      return MAX_LIGHT;
    }
    int block = Math.max(luminosity, (packedLight & 0xFFFF) >> 4) << 4;
    int sky = Math.max(luminosity, (packedLight >> 20) & 0xFFFF) << 20;
    return block | sky;
  }
}
