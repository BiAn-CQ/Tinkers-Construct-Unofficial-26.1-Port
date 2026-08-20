package slimeknights.tconstruct.library.client.data.util;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Logic to read sprites from existing images and return native images which can later be modified
 */
public class DataGenSpriteReader extends ResourceManagerSpriteReader {
  public DataGenSpriteReader(ResourceManager resourceManager, String folder) {
    super(resourceManager, folder);
  }
}
