package slimeknights.tconstruct.library.tools.layout;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import io.netty.handler.codec.DecoderException;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.data.loadable.common.ItemStackTemplateLoadable;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.utils.TinkerNetworkBuffer;

import javax.annotation.Nullable;

/** Data holder for a button icon, currently supports item stack icons and pattern icons */
public abstract class LayoutIcon {
  /** JSON serializer for a layout button icon */
  public static final Serializer SERIALIZER = new Serializer();

  /** Empty icon, used primarily as a fallback */
  public static final LayoutIcon EMPTY = new LayoutIcon() {
    @Nullable
    @Override
    public <T> T getValue(Class<T> clazz) {
      return null;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
      buffer.writeEnum(Type.EMPTY);
    }

    @Override
    public JsonObject toJson() {
      return new JsonObject();
    }
  };

  /** Creates a stack icon */
  public static LayoutIcon ofItem(ItemStack stack) {
    return new ItemStackIcon(ItemStackTemplate.fromNonEmptyStack(stack));
  }

  /** Creates an icon from a pattern */
  public static LayoutIcon ofPattern(Pattern pattern) {
    return new PatternIcon(pattern);
  }

  /** Gets the value of this icon, done this way to separate the drawing logic out */
  @Nullable
  public abstract <T> T getValue(Class<T> clazz);

  /** Reads the button icon from the buffer */
  public static LayoutIcon read(FriendlyByteBuf buffer) {
    Type type = buffer.readEnum(Type.class);
    switch (type) {
      case EMPTY: return EMPTY;
      case ITEM: {
        ItemStackTemplate template = ItemStackTemplate.STREAM_CODEC.decode(TinkerNetworkBuffer.registry(buffer));
        return new ItemStackIcon(template);
      }
      case PATTERN: {
        Pattern pattern = new Pattern(buffer.readIdentifier());
        return new PatternIcon(pattern);
      }
    }
    throw new DecoderException("Invalid LayoutButtonIcon " + type);
  }

  /** Writes this to the packet buffer */
  public abstract void write(FriendlyByteBuf buffer);

  /** Writes this object to json */
  public abstract JsonObject toJson();

  /** Icon drawing an item stack */
  @RequiredArgsConstructor @VisibleForTesting
  protected static class ItemStackIcon extends LayoutIcon {
    private final ItemStackTemplate template;

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getValue(Class<T> clazz) {
      if (clazz == ItemStack.class) {
        return (T) template.create();
      }
      return null;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
      buffer.writeEnum(Type.ITEM);
      ItemStackTemplate.STREAM_CODEC.encode(TinkerNetworkBuffer.registry(buffer), template);
    }

    @Override
    public JsonObject toJson() {
      return ItemStackTemplateLoadable.STACK.serialize(template).getAsJsonObject();
    }
  }

  /** Icon drawing a static patttern sprite */
  @RequiredArgsConstructor @VisibleForTesting
  protected static class PatternIcon extends LayoutIcon {
    private final Pattern pattern;

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getValue(Class<T> clazz) {
      if (clazz == Pattern.class) {
        return (T) pattern;
      }
      return null;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
      buffer.writeEnum(Type.PATTERN);
      buffer.writeIdentifier(pattern.location());
    }

    @Override
    public JsonObject toJson() {
      JsonObject json = new JsonObject();
      json.addProperty("pattern", pattern.toString());
      return json;
    }
  }

  /** enum of icon types for serialization */
  private enum Type {
    EMPTY,
    ITEM,
    PATTERN
  }

  /** Serializer class */
  protected static class Serializer implements JsonSerializer<LayoutIcon>, JsonDeserializer<LayoutIcon> {
    @Override
    public LayoutIcon deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      JsonObject object = GsonHelper.convertToJsonObject(json, "button_icon");
      if (object.has("pattern")) {
        Pattern pattern = new Pattern(JsonHelper.getIdentifier(object, "pattern"));
        return new PatternIcon(pattern);
      }
      if (object.has("id")) {
        return new ItemStackIcon(ItemStackTemplateLoadable.STACK.convert(object, "item", TypedMap.empty()));
      }
      // not sure why this would be needed, but might as well
      if (object.entrySet().isEmpty()) {
        return EMPTY;
      }
      throw new JsonSyntaxException("LayoutButtonIcon must have either pattern or a native item stack template");
    }

    @Override
    public JsonElement serialize(LayoutIcon icon, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
      return icon.toJson();
    }
  }
}
