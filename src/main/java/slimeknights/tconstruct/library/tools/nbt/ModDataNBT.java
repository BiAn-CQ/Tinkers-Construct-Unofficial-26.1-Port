package slimeknights.tconstruct.library.tools.nbt;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import slimeknights.tconstruct.library.utils.ResourceId;

import java.util.function.BiFunction;

/**
 * NBT representing extra data on the tool for modifiers, with a wrapper around the compound for to enforce namespacing data.
 * On a typical tool, there are two copies of this class, one for persistent data, and one that rebuilds when the modifiers refresh.
 * Note unlike other NBT classes, the data inside this one is mutable as most of it is directly used by the tools.
 */
@EqualsAndHashCode
public class ModDataNBT implements IModDataView {
  /** Compound representing modifier data */
  @Getter(AccessLevel.PROTECTED)
  private final CompoundTag data;
  @EqualsAndHashCode.Exclude
  private final Runnable onChanged;

  protected ModDataNBT(CompoundTag data) {
    this(data, () -> {});
  }

  protected ModDataNBT(CompoundTag data, Runnable onChanged) {
    this.data = data;
    this.onChanged = onChanged;
  }

  /**
   * Creates a new mod data containing empty data
   */
  public ModDataNBT() {
    this(new CompoundTag());
  }

  @Override
  public <T> T get(Identifier name, BiFunction<CompoundTag,String,T> function) {
    return function.apply(data, name.toString());
  }

  @Override
  public ListTag getList(Identifier name, int type) {
    // save generation of the extra lambda object
    return data.getListOrEmpty(name.toString());
  }

  @Override
  public boolean contains(Identifier name) {
    return data.contains(name.toString());
  }

  @Override
  public boolean contains(Identifier name, int type) {
    Tag value = data.get(name.toString());
    return value != null && (type == 99 ? value instanceof net.minecraft.nbt.NumericTag : value.getId() == type);
  }

  /**
   * Sets the given NBT into the data
   * @param name  Key name
   * @param nbt   NBT value
   */
  public void put(Identifier name, Tag nbt) {
    data.put(name.toString(), nbt);
    onChanged.run();
  }

  public void put(ResourceId name, Tag nbt) {
    put(name.location(), nbt);
  }

  /**
   * Sets an integer from the mod data
   * @param name  Name
   * @param value  Integer value
   */
  public void putInt(Identifier name, int value) {
    data.putInt(name.toString(), value);
    onChanged.run();
  }

  public void putInt(ResourceId name, int value) {
    putInt(name.location(), value);
  }

  /**
   * Sets an boolean from the mod data
   * @param name  Name
   * @param value  Boolean value
   */
  public void putBoolean(Identifier name, boolean value) {
    data.putBoolean(name.toString(), value);
    onChanged.run();
  }

  public void putBoolean(ResourceId name, boolean value) {
    putBoolean(name.location(), value);
  }

  /**
   * Sets an float from the mod data
   * @param name  Name
   * @param value  Float value
   */
  public void putFloat(Identifier name, float value) {
    data.putFloat(name.toString(), value);
    onChanged.run();
  }

  public void putFloat(ResourceId name, float value) {
    putFloat(name.location(), value);
  }

  /**
   * Reads a string from the mod data
   * @param name  Name
   * @param value  String value
   */
  public void putString(Identifier name, String value) {
    data.putString(name.toString(), value);
    onChanged.run();
  }

  public void putString(ResourceId name, String value) {
    putString(name.location(), value);
  }

  /**
   * Removes the given key from the NBT
   * @param name  Key to remove
   */
  public void remove(Identifier name) {
    data.remove(name.toString());
    onChanged.run();
  }

  public void remove(ResourceId name) {
    remove(name.location());
  }


  /* Networking */

  /** Gets a copy of the internal data, generally should only be used for syncing, no reason to call directly */
  public CompoundTag getCopy() {
    return data.copy();
  }

  /**
   * Called to merge this NBT data from another
   * @param data  data
   */
  public void copyFrom(CompoundTag data) {
    for (String key : this.data.keySet().toArray(String[]::new)) {
      this.data.remove(key);
    }
    this.data.merge(data);
    onChanged.run();
  }

  /** Marks direct subclass mutations as complete. */
  protected final void changed() {
    onChanged.run();
  }

  /**
   * Parses the data from NBT
   * @param data  data
   * @return  Parsed mod data
   */
  public static ModDataNBT readFromNBT(CompoundTag data) {
    return new ModDataNBT(data);
  }
}
