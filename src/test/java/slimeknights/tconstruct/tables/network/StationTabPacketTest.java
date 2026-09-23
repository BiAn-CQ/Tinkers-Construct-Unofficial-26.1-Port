package slimeknights.tconstruct.tables.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.tables.menu.TabbedContainerMenu;
import slimeknights.tconstruct.test.BaseMcTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StationTabPacketTest extends BaseMcTest {
  @Test
  void unloadedTabDoesNotDeleteCursorStack() {
    ServerPlayer player = mock(ServerPlayer.class);
    ServerLevel level = mock(ServerLevel.class);
    when(player.level()).thenReturn(level);
    var menu = spy(new TabbedContainerMenu<>(null, 0, null, null));
    doReturn(true).when(menu).stillValid(player);
    BlockPos pos = new BlockPos(1, 2, 3);
    menu.stationBlocks.add(Pair.of(pos, Blocks.STONE.defaultBlockState()));
    ItemStack held = new ItemStack(Items.DIAMOND, 3);
    menu.setCarried(held);
    player.containerMenu = menu;
    IPayloadContext context = mock(IPayloadContext.class);
    when(context.player()).thenReturn(player);

    new StationTabPacket(pos).handleThreadsafe(context);

    assertThat(menu.getCarried()).isSameAs(held);
    verify(player, never()).doCloseContainer();
  }

  @Test
  void positionOutsideCurrentTabsIsRejectedBeforeWorldLookup() {
    ServerPlayer player = mock(ServerPlayer.class);
    ServerLevel level = mock(ServerLevel.class);
    when(player.level()).thenReturn(level);
    var menu = spy(new TabbedContainerMenu<>(null, 0, null, null));
    doReturn(true).when(menu).stillValid(player);
    player.containerMenu = menu;
    IPayloadContext context = mock(IPayloadContext.class);
    when(context.player()).thenReturn(player);

    new StationTabPacket(new BlockPos(1000, 64, 1000)).handleThreadsafe(context);

    verify(level, never()).getBlockState(any());
    verify(player, never()).doCloseContainer();
  }
}
