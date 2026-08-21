package slimeknights.tconstruct.tables.block.entity.chest;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.block.ChestBlock;
import slimeknights.tconstruct.tables.block.entity.inventory.IChestItemHandler;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AbstractChestBlockEntityTest {
  private static final BlockPos POS = BlockPos.ZERO;

  @Test
  void partChestDropsContentsOnRemoval() {
    IChestItemHandler handler = mock(IChestItemHandler.class);
    TestChestBlockEntity chest = createChest(
      (ChestBlock)TinkerTables.partChest.get(), TinkerTables.partChestTile.get(), handler);

    chest.preRemoveSideEffects(POS, chest.getBlockState());

    verify(handler, atLeastOnce()).getSlots();
  }

  @Test
  void tinkersChestDropsContentsOnRemoval() {
    IChestItemHandler handler = mock(IChestItemHandler.class);
    TestChestBlockEntity chest = createChest(
      (ChestBlock)TinkerTables.tinkersChest.get(), TinkerTables.tinkersChestTile.get(), handler);

    chest.preRemoveSideEffects(POS, chest.getBlockState());

    verify(handler, atLeastOnce()).getSlots();
  }

  @Test
  void castChestKeepsContentsOnRemoval() {
    IChestItemHandler handler = mock(IChestItemHandler.class);
    TestChestBlockEntity chest = createChest(
      (ChestBlock)TinkerTables.castChest.get(), TinkerTables.castChestTile.get(), handler);

    chest.preRemoveSideEffects(POS, chest.getBlockState());

    verify(handler, never()).getSlots();
  }

  private static TestChestBlockEntity createChest(ChestBlock block, BlockEntityType<?> type, IChestItemHandler handler) {
    TestChestBlockEntity chest = new TestChestBlockEntity(type, block.defaultBlockState(), handler);
    chest.setLevel(mock(Level.class));
    return chest;
  }

  private static class TestChestBlockEntity extends AbstractChestBlockEntity {
    TestChestBlockEntity(BlockEntityType<?> type, BlockState state, IChestItemHandler handler) {
      super(type, POS, state, Component.empty(), handler);
    }
  }
}
