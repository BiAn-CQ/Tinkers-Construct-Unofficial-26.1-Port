package slimeknights.tconstruct.client.model;

import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.world.level.block.Block;
import slimeknights.mantle.client.model.util.ModelHelper;

import java.util.List;

/**
 * Extracts representative textures from a block for material-block models.
 *
 * <p>Material-block models intentionally use the source block's particle
 * texture as one opaque replacement texture. This matches the 1.20.1 anvil
 * renderer and avoids applying a translucent source layer to every retextured
 * face of the table or anvil.</p>
 */
final class MaterialBlockTextureHelper {
  private MaterialBlockTextureHelper() {}

  /** Gets the source block's particle texture as a normal opaque material. */
  static List<Material> getMaterials(Block block) {
    return List.of(new Material(ModelHelper.getParticleTexture(block)));
  }
}
