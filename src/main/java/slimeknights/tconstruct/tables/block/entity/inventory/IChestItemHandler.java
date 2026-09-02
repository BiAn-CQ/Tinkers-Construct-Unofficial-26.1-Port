package slimeknights.tconstruct.tables.block.entity.inventory;

import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import slimeknights.mantle.block.entity.MantleBlockEntity;

/** Interface for tinker chest TEs */
public interface IChestItemHandler extends ResourceHandler<ItemResource>, IndexModifier<ItemResource>, IScalingContainer, ValueIOSerializable {
  /** Sets the parent of this block */
  void setParent(MantleBlockEntity parent);
}
