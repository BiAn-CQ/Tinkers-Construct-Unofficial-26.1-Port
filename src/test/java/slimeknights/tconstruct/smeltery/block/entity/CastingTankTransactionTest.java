package slimeknights.tconstruct.smeltery.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity.ITankBlock;
import slimeknights.tconstruct.test.CoreTestBootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CastingTankTransactionTest extends CoreTestBootstrap {
  private static class TestTank extends CastingTankBlockEntity {
    int processCalls;

    TestTank(BlockEntityType<?> type, ITankBlock block) {
      super(type, BlockPos.ZERO, Blocks.AIR.defaultBlockState(), block);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
      return slot == INPUT && getItem(INPUT).isEmpty() && getItem(OUTPUT).isEmpty();
    }

    @Override
    protected void tryToProcessItem() {
      // The real fluid-container processing opens a root transaction too.
      try (Transaction transaction = Transaction.openRoot()) {
        processCalls++;
        transaction.commit();
      }
    }
  }

  private TestTank tank() {
    BlockEntityType<?> type = mock(BlockEntityType.class);
    when(type.isValid(any())).thenReturn(true);
    ITankBlock block = mock(ITankBlock.class);
    when(block.getCapacity()).thenReturn(4000);
    return new TestTank(type, block);
  }

  @Test
  void simulatedInsertionRollsBackWithoutProcessing() {
    TestTank tank = tank();
    try (Transaction transaction = Transaction.openRoot()) {
      assertThat(tank.getItemHandler().insert(0, ItemResource.of(Items.BUCKET), 1, transaction)).isEqualTo(1);
      assertThat(tank.processCalls).isZero();
    }
    assertThat(tank.getItem(0).isEmpty()).isTrue();
    assertThat(tank.processCalls).isZero();
  }

  @Test
  void committedInsertionProcessesAfterRootCloses() {
    TestTank tank = tank();
    try (Transaction transaction = Transaction.openRoot()) {
      assertThat(tank.getItemHandler().insert(0, ItemResource.of(Items.BUCKET), 1, transaction)).isEqualTo(1);
      assertThat(tank.processCalls).isZero();
      transaction.commit();
    }
    assertThat(tank.getItem(0).getItem()).isSameAs(Items.BUCKET);
    assertThat(tank.processCalls).isEqualTo(1);
  }

  @Test
  void committedChildStillRollsBackWithParent() {
    TestTank tank = tank();
    try (Transaction parent = Transaction.openRoot()) {
      try (Transaction child = Transaction.open(parent)) {
        tank.getItemHandler().insert(0, ItemResource.of(Items.BUCKET), 1, child);
        child.commit();
      }
      assertThat(tank.processCalls).isZero();
    }
    assertThat(tank.getItem(0).isEmpty()).isTrue();
    assertThat(tank.processCalls).isZero();
  }
}
