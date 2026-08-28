package slimeknights.tconstruct.library.tools.helper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.context.ToolHarvestContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ToolHarvestLifecycleTest {
  @Test
  void defaultRemovalRunsPlayerWillDestroyAndUsesReturnedState() {
    IToolStackView tool = mock(IToolStackView.class);
    ItemStack stack = mock(ItemStack.class);
    ToolHarvestContext context = mock(ToolHarvestContext.class);
    ServerLevel level = mock(ServerLevel.class);
    ServerPlayer player = mock(ServerPlayer.class);
    BlockState originalState = mock(BlockState.class);
    BlockState destroyState = mock(BlockState.class);
    Block originalBlock = mock(Block.class);
    Block destroyBlock = mock(Block.class);
    FluidState fluid = mock(FluidState.class);
    BlockPos pos = BlockPos.ZERO;

    when(context.getWorld()).thenReturn(level);
    when(context.getPlayer()).thenReturn(player);
    when(context.getPos()).thenReturn(pos);
    when(context.getState()).thenReturn(originalState);
    when(originalState.getBlock()).thenReturn(originalBlock);
    when(originalBlock.playerWillDestroy(level, pos, originalState, player)).thenReturn(destroyState);
    when(destroyState.getBlock()).thenReturn(destroyBlock);
    when(level.getFluidState(pos)).thenReturn(fluid);
    when(destroyState.onDestroyedByPlayer(level, pos, player, stack, false, fluid)).thenReturn(true);

    assertThat(ToolHarvestLogic.removeBlock(tool, stack, context)).isSameAs(destroyState);
    verify(originalBlock).playerWillDestroy(level, pos, originalState, player);
    verify(destroyState).onDestroyedByPlayer(level, pos, player, stack, false, fluid);
    verify(destroyBlock).destroy(level, pos, destroyState);
  }

}
