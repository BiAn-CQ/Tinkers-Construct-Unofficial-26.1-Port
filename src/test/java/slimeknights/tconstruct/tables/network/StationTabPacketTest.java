package slimeknights.tconstruct.tables.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.tables.block.ITabbedBlock;
import static org.mockito.Mockito.*;

class StationTabPacketTest {
  @Test void changingTabClosesServerMenuWithoutClosingClientScreen() {
    var player = mock(ServerPlayer.class);
    var world = mock(ServerLevel.class);
    var menu = mock(AbstractContainerMenu.class);
    player.containerMenu = menu;
    when(menu.getCarried()).thenReturn(ItemStack.EMPTY);
    when(player.level()).thenReturn(world);
    when(world.hasChunkAt(BlockPos.ZERO)).thenReturn(true);
    var state = mock(BlockState.class);
    var block = mock(Block.class, withSettings().extraInterfaces(ITabbedBlock.class));
    when(world.getBlockState(BlockPos.ZERO)).thenReturn(state);
    when(state.getBlock()).thenReturn(block);
    var context = mock(IPayloadContext.class);
    when(context.player()).thenReturn(player);
    new StationTabPacket(BlockPos.ZERO).handleThreadsafe(context);
    var order = inOrder(player, block);
    order.verify(player).doCloseContainer();
    order.verify((ITabbedBlock)block).openGui(player, world, BlockPos.ZERO);
    verify(player, never()).closeContainer();
  }
}
