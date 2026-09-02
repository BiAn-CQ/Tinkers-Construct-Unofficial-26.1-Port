package slimeknights.tconstruct.tables.block.entity.chest;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.block.entity.inventory.ScalingChestItemHandler;

/**
 * Chest that holds casts, up to 4 of every type
 */
public class CastChestBlockEntity extends AbstractChestBlockEntity {
  private static final Component NAME = TConstruct.makeTranslation("gui", "cast_chest");
  public CastChestBlockEntity(BlockPos pos, BlockState state) {
    super(TinkerTables.castChestTile.get(), pos, state, NAME, new CastChestItemHandler());
  }

  /** Item handler for cast chests */
  public static class CastChestItemHandler extends ScalingChestItemHandler {
    @Override
    protected int getCapacity(int slot, ItemResource resource) {
      return 4;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
      for (int i = 0; i < this.size(); i++) {
        if (ItemStack.isSameItem(stack, ItemUtil.getStack(this, i))) {
          return i == slot;
        }
      }
      return stack.is(TinkerTags.Items.CASTS);
    }
  }
}
