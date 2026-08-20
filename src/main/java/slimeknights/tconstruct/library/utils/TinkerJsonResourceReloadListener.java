package slimeknights.tconstruct.library.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;

/**
 * Compatibility wrapper for Tinkers JSON reloaders.
 *
 * <p>26.1 changed vanilla's JSON listener constructor from Gson and a folder
 * string to a codec and {@link FileToIdConverter}. Tinkers still intentionally
 * keeps Gson in its managers because their formats use custom adapters and
 * legacy loadables, so this wrapper only adapts the reload discovery step.</p>
 */
public abstract class TinkerJsonResourceReloadListener extends SimpleJsonResourceReloadListener<JsonElement> {
  public static final Codec<JsonElement> JSON_ELEMENT_CODEC = Codec.PASSTHROUGH.xmap(
    dynamic -> (JsonElement)dynamic.getValue(),
    json -> new com.mojang.serialization.Dynamic<>(com.mojang.serialization.JsonOps.INSTANCE, json));

  protected TinkerJsonResourceReloadListener(Gson gson, String folder) {
    super(JSON_ELEMENT_CODEC, FileToIdConverter.json(folder));
  }
}
