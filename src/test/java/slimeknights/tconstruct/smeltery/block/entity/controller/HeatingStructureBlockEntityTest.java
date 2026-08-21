package slimeknights.tconstruct.smeltery.block.entity.controller;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer.IOreRate;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModuleInventory;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.HeatingStructureMultiblock;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.HeatingStructureMultiblock.StructureData;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HeatingStructureBlockEntityTest {
  @Test
  void removalInvalidatesStructureAndDropsUnmeltedItems() {
    BlockEntityType<?> type = mock(BlockEntityType.class);
    when(type.isValid(any())).thenReturn(true);
    BlockState state = Blocks.FURNACE.defaultBlockState();
    TestHeatingStructure controller = new TestHeatingStructure(type, BlockPos.ZERO, state);

    Level level = mock(Level.class);
    when(level.getRandom()).thenReturn(mock(RandomSource.class));
    controller.setLevel(level);

    StructureData structure = mock(StructureData.class);
    controller.setStructureForTest(structure);
    controller.getMeltingInventory().resize(1, stack -> {});
    ItemStack stored = mock(ItemStack.class);
    AtomicBoolean empty = new AtomicBoolean(false);
    when(stored.isEmpty()).thenAnswer(invocation -> empty.get());
    when(stored.getCount()).thenReturn(1);
    when(stored.split(anyInt())).thenAnswer(invocation -> {
      empty.set(true);
      return mock(ItemStack.class);
    });
    controller.getMeltingInventory().setStackInSlot(0, stored);

    controller.preRemoveSideEffects(BlockPos.ZERO, state);

    verify(structure).clearMaster(controller);
    assertThat(controller.getStructure()).isNull();
    assertThat(controller.getMeltingInventory().getStackInSlot(0).isEmpty()).isTrue();
    verify(level).addFreshEntity(any(Entity.class));
  }

  private static class TestHeatingStructure extends HeatingStructureBlockEntity {
    @SuppressWarnings({"unchecked", "rawtypes"})
    TestHeatingStructure(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super((BlockEntityType)type, pos, state, Component.empty());
    }

    @Override
    protected HeatingStructureMultiblock<?> createMultiblock() {
      return mock(HeatingStructureMultiblock.class);
    }

    @Override
    protected MeltingModuleInventory createMeltingInventory() {
      return new MeltingModuleInventory(this, tank, mock(IOreRate.class));
    }

    @Override
    protected void heat() {}

    @Override
    protected boolean isDebugItem(ItemStack stack) {
      return false;
    }

    void setStructureForTest(StructureData structure) {
      this.structure = structure;
    }
  }
}
