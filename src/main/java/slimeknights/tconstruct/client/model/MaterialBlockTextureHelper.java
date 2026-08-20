package slimeknights.tconstruct.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import slimeknights.mantle.client.model.util.ModelHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts representative textures from a block for material-block models.
 *
 * <p>A particle texture is sufficient for ordinary blocks, but it flattens a
 * composite block into one opaque image. If the source model has both opaque
 * and translucent geometry, retain one texture from each layer so retextured
 * tables and anvils preserve the source block's layered appearance.</p>
 */
final class MaterialBlockTextureHelper {
  private MaterialBlockTextureHelper() {}

  /** Gets either the normal particle material or the source model's opaque/translucent pair. */
  static List<Material> getMaterials(Block block) {
    BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(block.defaultBlockState());
    List<BlockStateModelPart> parts = new ArrayList<>();
    model.collectParts(RandomSource.create(0), parts);

    Identifier opaque = null;
    Identifier translucent = null;
    for (BlockStateModelPart part : parts) {
      for (Direction direction : Direction.values()) {
        Identifier[] textures = findLayerTextures(part.getQuads(direction), opaque, translucent);
        opaque = textures[0];
        translucent = textures[1];
        if (opaque != null && translucent != null) {
          return List.of(new Material(opaque), new Material(translucent, true));
        }
      }
      Identifier[] textures = findLayerTextures(part.getQuads(null), opaque, translucent);
      opaque = textures[0];
      translucent = textures[1];
      if (opaque != null && translucent != null) {
        return List.of(new Material(opaque), new Material(translucent, true));
      }
    }

    if (translucent != null) {
      return List.of(new Material(translucent, true));
    }
    return List.of(new Material(ModelHelper.getParticleTexture(block)));
  }

  /** Returns the first texture encountered for each rendering class. */
  private static Identifier[] findLayerTextures(List<BakedQuad> quads, Identifier opaque, Identifier translucent) {
    for (BakedQuad quad : quads) {
      Identifier texture = quad.materialInfo().sprite().contents().name();
      if (quad.materialInfo().layer().translucent()) {
        if (translucent == null) {
          translucent = texture;
        }
      } else if (opaque == null) {
        opaque = texture;
      }
      if (opaque != null && translucent != null) {
        break;
      }
    }
    return new Identifier[] { opaque, translucent };
  }
}
