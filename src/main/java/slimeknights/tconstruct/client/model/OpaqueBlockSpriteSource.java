package slimeknights.tconstruct.client.model;

import com.mojang.serialization.MapCodec;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import slimeknights.tconstruct.TConstruct;

/** Opaque copies for solid models reusing block textures with transparent pixels. */
public record OpaqueBlockSpriteSource() implements SpriteSource {
  public static final MapCodec<OpaqueBlockSpriteSource> CODEC = MapCodec.unit(new OpaqueBlockSpriteSource());

  public static Identifier opaqueId(Identifier source) {
    return TConstruct.getResource("opaque_block/" + source.getNamespace() + "/" + source.getPath());
  }

  @Override
  public void run(ResourceManager manager, Output output) {
    manager.listResources("textures/block", id -> id.getPath().endsWith(".png")).forEach((file, resource) -> {
      Identifier destination = opaqueId(TEXTURE_ID_CONVERTER.fileToId(file));
      output.add(destination, loader -> loader.loadSprite(destination, resource, (id, size, image, animation, metadata, texture) -> {
        if (image.computeTransparency().isOpaque()) {
          image.close();
          return null;
        }
        fillTransparentPixels(image, size.width(), size.height());
        return new SpriteContents(id, size, image, animation, metadata, texture);
      }));
    });
  }

  static void fillTransparentPixels(NativeImage image, int frameWidth, int frameHeight) {
    int[] original = new int[image.getWidth() * image.getHeight()];
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        original[y * image.getWidth() + x] = image.getPixel(x, y);
      }
    }
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        int pixel = original[y * image.getWidth() + x];
        if ((pixel >>> 24) == 0) {
          int left = x / frameWidth * frameWidth;
          int top = y / frameHeight * frameHeight;
          int nearest = Integer.MAX_VALUE;
          int red = 0, green = 0, blue = 0, count = 0;
          for (int sy = top; sy < top + frameHeight; sy++) {
            for (int sx = left; sx < left + frameWidth; sx++) {
              int sample = original[sy * image.getWidth() + sx];
              if ((sample >>> 24) == 0) continue;
              int distance = (sx - x) * (sx - x) + (sy - y) * (sy - y);
              if (distance > nearest) continue;
              if (distance < nearest) {
                nearest = distance;
                red = green = blue = count = 0;
              }
              red += sample >> 16 & 255;
              green += sample >> 8 & 255;
              blue += sample & 255;
              count++;
            }
          }
          if (count > 0) pixel = (red / count << 16) | (green / count << 8) | blue / count;
        }
        image.setPixel(x, y, pixel | 0xFF000000);
      }
    }
  }

  @Override
  public MapCodec<? extends SpriteSource> codec() {
    return CODEC;
  }
}
