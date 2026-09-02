package slimeknights.tconstruct.tables.block.entity.inventory;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import slimeknights.mantle.block.entity.MantleBlockEntity;

import javax.annotation.Nullable;

/** Base logic for scaling chest inventories */
public abstract class ScalingChestItemHandler extends ItemStacksResourceHandler implements IChestItemHandler {
  /** Default maximum size */
  protected static final int DEFAULT_MAX = 256;
  /** Current size for display in containers */
  @Getter
  private int visualSize = 1;
  /** TE owning this inventory */
  @Setter @Nullable
  private MantleBlockEntity parent;

  public ScalingChestItemHandler(int size) {
    super(size);
  }

  public ScalingChestItemHandler() {
    this(DEFAULT_MAX);
  }

  public abstract boolean isItemValid(int slot, ItemStack stack);

  @Override
  public boolean isValid(int index, ItemResource resource) {
    return !resource.isEmpty() && isItemValid(index, resource.toStack());
  }

  @Override
  public void deserialize(ValueInput input) {
    super.deserialize(input);
    int newLimit = size();
    if (newLimit > 1 && ItemUtil.getStack(this, newLimit - 1).isEmpty()) {
      while (newLimit > 1 && ItemUtil.getStack(this, newLimit - 2).isEmpty()) {
        newLimit--;
      }
    }
    this.visualSize = newLimit;
  }

  /** Updates the visual size of the inventory */
  private void updateVisualSize(int slotChanged, ItemStack stack) {
    // if the slot is too large, nothing to do
    int maxSlots = size();
    if (slotChanged >= maxSlots) {
      return;
    }
    // if the slot is past the current one, update to there
    if (stack.isEmpty()) {
      // if the current index was the last slot, decrease size
      if (slotChanged + 1 == visualSize || (slotChanged + 2 == visualSize && ItemUtil.getStack(this, visualSize - 1).isEmpty())) {
        while (visualSize > 1 && ItemUtil.getStack(this, visualSize - 2).isEmpty()) {
          visualSize--;
        }
      }
    } else {
      // if the current index is past the max, increase visual size to this plus 1
      if (visualSize < maxSlots && visualSize < slotChanged + 2) {
        visualSize =slotChanged + 2;
      }
    }
  }

  @Override
  protected void onContentsChanged(int slot, ItemStack previousContents) {
    updateVisualSize(slot, stacks.get(slot));
    if (parent != null) {
      parent.setChangedFast();
    }
  }
}
