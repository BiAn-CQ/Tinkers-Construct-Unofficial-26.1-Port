package slimeknights.tconstruct.client.model;

import com.mojang.blaze3d.platform.Transparency;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MaterialBlockTextureHelperTest {
  @Test
  void transparentBlockKeepsItsOwnTextureUsingOpaqueCopy() {
    var material = new Material(Identifier.parse("test:block/metal"));
    var opaque = new Material(OpaqueBlockSpriteSource.opaqueId(material.sprite()));
    var sprite = mock(TextureAtlasSprite.class);
    var contents = mock(SpriteContents.class);
    when(sprite.contents()).thenReturn(contents);
    when(contents.transparency()).thenReturn(Transparency.TRANSPARENT);
    var solidSprite = mock(TextureAtlasSprite.class);
    var solidContents = mock(SpriteContents.class);
    when(solidSprite.contents()).thenReturn(solidContents);
    when(solidContents.name()).thenReturn(opaque.sprite());
    assertThat(MaterialBlockTextureHelper.opaque(material, value -> value.equals(material) ? sprite : solidSprite)).isEqualTo(opaque);
    when(contents.transparency()).thenReturn(Transparency.NONE);
    assertThat(MaterialBlockTextureHelper.opaque(material, value -> sprite)).isSameAs(material);
  }
  @Test
  void transparentPixelsUseNeighborsFromTheSameAnimationFrame() {
    try (var image = new com.mojang.blaze3d.platform.NativeImage(3, 2, true)) {
      image.setPixel(0, 0, 0xFF809060);
      image.setPixel(1, 0, 0);
      image.setPixel(2, 0, 0xFF809060);
      image.setPixel(0, 1, 0xFF102030);
      image.setPixel(1, 1, 0);
      image.setPixel(2, 1, 0xFF102030);
      OpaqueBlockSpriteSource.fillTransparentPixels(image, 3, 1);
      assertThat(image.getPixel(1, 0)).isEqualTo(0xFF809060);
      assertThat(image.getPixel(1, 1)).isEqualTo(0xFF102030);
      assertThat(image.getPixel(0, 0)).isEqualTo(0xFF809060);
    }
  }
}
