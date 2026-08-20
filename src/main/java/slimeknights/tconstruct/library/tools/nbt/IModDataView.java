package slimeknights.tconstruct.library.tools.nbt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.utils.ResourceId;

import java.util.function.BiFunction;

/**
 * Read only view of {@link ModDataNBT}
 */
public interface IModDataView {
  /** Empty variant of tool data */
  IModDataView EMPTY = new IModDataView() {
    @Override
    public <T> T get(Identifier name, BiFunction<CompoundTag,String,T> function) {
      return function.apply(new CompoundTag(), name.toString());
    }

    @Override
    public boolean contains(Identifier name) {
      return false;
    }

    @Override
    public boolean contains(Identifier name, int type) {
      return false;
    }
  };

  /**
   * Gets a namespaced key from NBT
   * @param name      Namedspaced key
   * @param function  Function to get data using the key
   * @param <T>  NBT type of output
   * @return  Data based on the function
   */
  <T> T get(Identifier name, BiFunction<CompoundTag,String,T> function);

  default <T> T get(ResourceId name, BiFunction<CompoundTag,String,T> function) {
    return get(name.location(), function);
  }

  /**
   * Checks if the data contains the given tag with any type.
   * Generally, its better to use {@link #contains(Identifier, int)}, but there are rare benefits to this method.
   * @param name  Namespaced key
   * @return  True if the tag is contained
   */
  boolean contains(Identifier name);

  default boolean contains(ResourceId name) {
    return contains(name.location());
  }

  /**
   * Checks if the data contains the given tag
   * @param name  Namespaced key
   * @param type  Tag type, see {@link Tag} for values
   * @return  True if the tag is contained
   */
  boolean contains(Identifier name, int type);

  default boolean contains(ResourceId name, int type) {
    return contains(name.location(), type);
  }

  /**
   * Gets the number of slots provided by this data. Will be 0 if this data does not support slots.
   * @param type  Type of slot to get
   * @return  Number of slots
   */
  default int getSlots(SlotType type) {
    return 0;
  }


  /* Helpers */

  /**
   * Reads an generic NBT value from the mod data
   * @param name  Name
   * @return  Integer value
   */
  default Tag get(Identifier name) {
    return get(name, CompoundTag::get);
  }

  default Tag get(ResourceId name) {
    return get(name.location());
  }

  /**
   * Reads an integer from the mod data
   * @param name  Name
   * @return  Integer value
   */
  default int getInt(Identifier name) {
    return get(name, (tag, key) -> tag.getIntOr(key, 0));
  }

  default int getInt(ResourceId name) {
    return getInt(name.location());
  }

  /** Reads an integer, returning the supplied value when the key is absent. */
  default int getIntOr(Identifier name, int defaultValue) {
    return get(name, (tag, key) -> tag.getIntOr(key, defaultValue));
  }

  default int getIntOr(ResourceId name, int defaultValue) {
    return getIntOr(name.location(), defaultValue);
  }

  /**
   * Reads an boolean from the mod data
   * @param name  Name
   * @return  Boolean value
   */
  default boolean getBoolean(Identifier name) {
    return get(name, (tag, key) -> tag.getBooleanOr(key, false));
  }

  default boolean getBoolean(ResourceId name) {
    return getBoolean(name.location());
  }

  /** Reads a boolean, returning the supplied value when the key is absent. */
  default boolean getBooleanOr(Identifier name, boolean defaultValue) {
    return get(name, (tag, key) -> tag.getBooleanOr(key, defaultValue));
  }

  default boolean getBooleanOr(ResourceId name, boolean defaultValue) {
    return getBooleanOr(name.location(), defaultValue);
  }

  /**
   * Reads an float from the mod data
   * @param name  Name
   * @return  Float value
   */
  default float getFloat(Identifier name) {
    return get(name, (tag, key) -> tag.getFloatOr(key, 0f));
  }

  default float getFloat(ResourceId name) {
    return getFloat(name.location());
  }

  /** Reads a float, returning the supplied value when the key is absent. */
  default float getFloatOr(Identifier name, float defaultValue) {
    return get(name, (tag, key) -> tag.getFloatOr(key, defaultValue));
  }

  default float getFloatOr(ResourceId name, float defaultValue) {
    return getFloatOr(name.location(), defaultValue);
  }

  /**
   * Reads a string from the mod data
   * @param name  Name
   * @return  String value
   */
  default String getString(Identifier name) {
    return get(name, (tag, key) -> tag.getStringOr(key, ""));
  }

  default String getString(ResourceId name) {
    return getString(name.location());
  }

  /** Reads a string, returning the supplied value when the key is absent. */
  default String getStringOr(Identifier name, String defaultValue) {
    return get(name, (tag, key) -> tag.getStringOr(key, defaultValue));
  }

  default String getStringOr(ResourceId name, String defaultValue) {
    return getStringOr(name.location(), defaultValue);
  }

  /**
   * Reads a compound from the mod data
   * @param name  Name
   * @return  Compound value
   */
  default CompoundTag getCompound(Identifier name) {
    return get(name, CompoundTag::getCompoundOrEmpty);
  }

  default CompoundTag getCompound(ResourceId name) {
    return getCompound(name.location());
  }

  /** Reads a compound from the mod data, retaining the explicit 26.1 helper name used by callers. */
  default CompoundTag getCompoundOrEmpty(Identifier name) {
    return getCompound(name);
  }

  default CompoundTag getCompoundOrEmpty(ResourceId name) {
    return getCompound(name);
  }

  /**
   * Reads a list from the mod data
   * @param name  Name
   * @param type  List type
   * @return  List value
   */
  default ListTag getList(Identifier name, int type) {
    return get(name, (tag, key) -> tag.getListOrEmpty(key));
  }

  default ListTag getList(ResourceId name, int type) {
    return getList(name.location(), type);
  }
}
