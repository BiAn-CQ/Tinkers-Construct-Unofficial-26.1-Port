package slimeknights.tconstruct.tables.block.entity.inventory;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import slimeknights.mantle.block.entity.MantleBlockEntity;

/** Interface for tinker chest TEs */
public interface IChestItemHandler extends IItemHandlerModifiable, IScalingContainer, ValueIOSerializable {
  /** Sets the parent of this block */
  void setParent(MantleBlockEntity parent);
}
