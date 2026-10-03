package slimeknights.tconstruct.client.model;

import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.Block;
import slimeknights.mantle.client.model.util.ModelHelper;
import java.util.List;
import java.util.function.Function;

final class MaterialBlockTextureHelper {
  private MaterialBlockTextureHelper() {}

  static List<Material> getMaterials(Block block) {
    return List.of(new Material(ModelHelper.getParticleTexture(block)));
  }

  static Material opaque(Material material, Function<Material,TextureAtlasSprite> sprites) {
    if (sprites.apply(material).contents().transparency().isOpaque()) {
      return material;
    }
    Material opaque = new Material(OpaqueBlockSpriteSource.opaqueId(material.sprite()));
    return sprites.apply(opaque).contents().name().equals(MissingTextureAtlasSprite.getLocation()) ? material : opaque;
  }
}
