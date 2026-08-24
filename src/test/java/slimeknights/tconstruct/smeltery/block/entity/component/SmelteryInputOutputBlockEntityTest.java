package slimeknights.tconstruct.smeltery.block.entity.component;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.common.multiblock.IMasterLogic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmelteryInputOutputBlockEntityTest {
  @Test
  @SuppressWarnings("unchecked")
  void replacingMasterAtSamePositionInvalidatesExposedCapability() {
    BlockEntityType<?> type = mock(BlockEntityType.class);
    when(type.isValid(any())).thenReturn(true);
    BlockCapability<Object,Direction> capability = mock(BlockCapability.class);
    TestInputOutput servant = new TestInputOutput(type, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState(), capability);

    Level level = mock(Level.class);
    servant.setLevel(level);
    BlockPos masterPos = new BlockPos(2, 3, 4);
    servant.setInitialMaster(masterPos, Blocks.BLAST_FURNACE);
    clearInvocations(level);

    IMasterLogic replacement = mock(IMasterLogic.class);
    when(replacement.getMasterPos()).thenReturn(masterPos);
    when(replacement.getMasterBlock()).thenReturn(Blocks.BLAST_FURNACE.defaultBlockState());
    servant.setPotentialMaster(replacement);

    verify(level).invalidateCapabilities(BlockPos.ZERO);
  }

  private static class TestInputOutput extends SmelteryInputOutputBlockEntity<Object> {
    TestInputOutput(BlockEntityType<?> type, BlockPos pos, BlockState state, BlockCapability<Object,Direction> capability) {
      super(type, pos, state, capability);
    }

    void setInitialMaster(BlockPos master, Block block) {
      setMaster(master, block);
    }
  }
}
