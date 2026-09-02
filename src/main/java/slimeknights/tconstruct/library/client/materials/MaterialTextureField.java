package slimeknights.tconstruct.library.client.materials;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import slimeknights.mantle.data.loadable.field.RecordField;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;

import javax.annotation.Nullable;
import java.util.Objects;

/** Field handling the three state material texture */
enum MaterialTextureField implements RecordField<Identifier, MaterialRenderInfo> {
  INSTANCE;

  @Nullable
  @Override
  public Identifier get(JsonObject json, TypedMap context) {
    // if we have a texture, use that
    if (json.has("texture")) {
      return JsonHelper.getIdentifier(json, "texture", null);
    }
    return Objects.requireNonNull(context.get(MaterialVariantId.CONTEXT_KEY),
      "Unable to fetch material variant from context, this usually implements a broken JSON deserializer").getLocation('_');
  }

  @Override
  public void serialize(MaterialRenderInfo parent, JsonObject json) {
    Identifier texture = parent.texture();
    if (texture == null) {
      json.add("texture", JsonNull.INSTANCE);
    } else if (!texture.equals(parent.id().getLocation('_'))) {
      json.addProperty("texture", texture.toString());
    }
  }

  /** Enum representing options for the texture */
  private enum TextureType {
    /** Uses the material ID for texture */
    DEFAULT,
    /** No texture */
    NONE,
    /** Uses a specific texture name */
    NAME
  }

  @Nullable
  @Override
  public Identifier decode(FriendlyByteBuf buffer, TypedMap context) {
    return switch (buffer.readEnum(TextureType.class)) {
      case NONE -> null;
      case DEFAULT -> Objects.requireNonNull(context.get(MaterialVariantId.CONTEXT_KEY)).getLocation('_');
      case NAME -> buffer.readIdentifier();
    };
  }

  @Override
  public void encode(FriendlyByteBuf buffer, MaterialRenderInfo parent) {
    Identifier texture = parent.texture();
    // save some network traffic if the texture is the ID, since we already need an extra byte to specify null
    if (texture == null) {
      buffer.writeEnum(TextureType.NONE);
    } else if (texture.equals(parent.id().getLocation('_'))) {
      buffer.writeEnum(TextureType.DEFAULT);
    } else {
      buffer.writeEnum(TextureType.NAME);
      buffer.writeIdentifier(texture);
    }
  }
}
