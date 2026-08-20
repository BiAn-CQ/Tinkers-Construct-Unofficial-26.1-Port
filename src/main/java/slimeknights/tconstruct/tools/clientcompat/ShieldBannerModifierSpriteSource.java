package slimeknights.tconstruct.tools.clientcompat;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.sources.LazyLoadedImage;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.client.event.RegisterSpriteSourcesEvent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;

import java.io.IOException;
import java.util.Objects;

/** Generates banner modifier sprites from the vanilla shield atlas entries. */
public record ShieldBannerModifierSpriteSource(int cropX, int cropY, int cropWidth, int cropHeight,
                                               Identifier destinationPrefix, int offsetX, int offsetY, int outSize)
    implements SpriteSource {
  private static final Codec<Integer> NON_NEGATIVE = ExtraCodecs.intRange(0, Integer.MAX_VALUE);
  private static final Codec<Integer> SHIELD_SIZE = ExtraCodecs.intRange(0, 64);
  public static final MapCodec<ShieldBannerModifierSpriteSource> CODEC = RecordCodecBuilder.<ShieldBannerModifierSpriteSource>mapCodec(inst -> inst.group(
    SHIELD_SIZE.fieldOf("crop_x").forGetter(ShieldBannerModifierSpriteSource::cropX),
    SHIELD_SIZE.fieldOf("crop_y").forGetter(ShieldBannerModifierSpriteSource::cropY),
    SHIELD_SIZE.fieldOf("crop_width").forGetter(ShieldBannerModifierSpriteSource::cropWidth),
    SHIELD_SIZE.fieldOf("crop_height").forGetter(ShieldBannerModifierSpriteSource::cropHeight),
    Identifier.CODEC.fieldOf("destination_prefix").forGetter(ShieldBannerModifierSpriteSource::destinationPrefix),
    NON_NEGATIVE.fieldOf("offset_x").forGetter(ShieldBannerModifierSpriteSource::offsetX),
    NON_NEGATIVE.fieldOf("offset_y").forGetter(ShieldBannerModifierSpriteSource::offsetY),
    NON_NEGATIVE.fieldOf("output_size").forGetter(ShieldBannerModifierSpriteSource::outSize)
  ).apply(inst, ShieldBannerModifierSpriteSource::new)).validate(source -> {
    if (source.cropX + source.cropWidth >= 64 || source.cropY + source.cropHeight >= 64) {
      return DataResult.error(() -> "Invalid banner shield modifier sprite source: crop region must be within 64 by 64");
    }
    if (source.offsetX + source.cropWidth >= source.outSize || source.offsetY + source.cropHeight >= source.outSize) {
      return DataResult.error(() -> "Invalid banner shield modifier sprite source: crop result must fit within output size " + source.outSize);
    }
    return DataResult.success(source);
  });

  @Internal
  public static void register(RegisterSpriteSourcesEvent event) {
    event.register(TConstruct.getResource("shield_banner_to_modifier"), CODEC);
  }

  @Override
  public void run(ResourceManager manager, Output output) {
    manager.listResources("textures/entity/shield", id -> id.getPath().endsWith(".png")).forEach((input, resource) -> {
      Identifier texture = TEXTURE_ID_CONVERTER.fileToId(input);
      String path = texture.getPath();
      String prefix = "entity/shield/";
      if (path.startsWith(prefix) && !path.equals(prefix + "base")) {
        Identifier pattern = texture.withPath(path.substring(prefix.length()));
        LazyLoadedImage image = new LazyLoadedImage(input, resource, 1);
        Identifier destination = destinationPrefix.withSuffix(MaterialRenderInfo.getSuffix(pattern));
        output.add(destination, new BannerModifierSpriteSupplier(image, input, destination));
      }
    });
  }

  @Override
  public MapCodec<? extends SpriteSource> codec() {
    return CODEC;
  }

  private final class BannerModifierSpriteSupplier implements SpriteSource.DiscardableLoader {
    private final LazyLoadedImage original;
    private final Identifier input;
    private final Identifier output;
    private boolean released;

    private BannerModifierSpriteSupplier(LazyLoadedImage original, Identifier input, Identifier output) {
      this.original = original;
      this.input = input;
      this.output = output;
    }

    @Nullable
    @Override
    public SpriteContents get(SpriteResourceLoader loader) {
      try {
        NativeImage source = original.get();
        int scale = source.getWidth() / 64;
        if (scale == 0) {
          TConstruct.LOG.warn("Unable to crop {} to produce {} as texture is less than 64 pixels", input, output);
          return null;
        }
        NativeImage generated = new NativeImage(outSize * scale, outSize * scale, true);
        source.copyRect(generated, cropX * scale, cropY * scale, offsetX * scale, offsetY * scale,
                        cropWidth * scale, cropHeight * scale, false, false);
        return new SpriteContents(output, new FrameSize(generated.getWidth(), generated.getHeight()), generated);
      } catch (IllegalArgumentException | IOException exception) {
        TConstruct.LOG.warn("Unable to crop {} to produce {}", input, output, exception);
        return null;
      } finally {
        release();
      }
    }

    @Override
    public void discard() {
      release();
    }

    private synchronized void release() {
      if (!released) {
        released = true;
        original.release();
      }
    }
  }
}
