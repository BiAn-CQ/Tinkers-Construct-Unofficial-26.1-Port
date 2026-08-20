package slimeknights.tconstruct.library.tools.nbt;

import net.minecraft.nbt.CompoundTag;
import slimeknights.tconstruct.library.tools.SlotType;

/**
 * Extension of {@link ModDataNBT} adding support for modifier slots.
 * Typically, slots are only directly modifiable by the tool, though all contexts are free to view them.
 */
public class ToolDataNBT extends ModDataNBT {
  public ToolDataNBT() {}

  protected ToolDataNBT(CompoundTag nbt) {
    super(nbt);
  }

  protected ToolDataNBT(CompoundTag nbt, Runnable onChanged) {
    super(nbt, onChanged);
  }

  @Override
  public int getSlots(SlotType type) {
    return getData().getIntOr(type.getName(), 0);
  }

  /**
   * Sets the slots for the given type
   * @param type   Slot type
   * @param value  New value
   */
  public void setSlots(SlotType type, int value) {
    if (value == 0) {
      getData().remove(type.getName());
    } else {
      getData().putInt(type.getName(), value);
    }
    changed();
  }

  /**
   * Adds the given number of slots
   * @param type   Slot type
   * @param add    Value to add, use negative to remove
   */
  public void addSlots(SlotType type, int add) {
    if (add != 0) {
      setSlots(type, getSlots(type) + add);
    }
  }


  /**
   * Parses the mod data from NBT
   * @param data  data
   * @return  Parsed mod data
   */
  public static ToolDataNBT readFromNBT(CompoundTag data) {
    return new ToolDataNBT(data);
  }

  /** Parses mutable tool data and invokes the callback after each mutation. */
  public static ToolDataNBT readFromNBT(CompoundTag data, Runnable onChanged) {
    return new ToolDataNBT(data, onChanged);
  }
}
