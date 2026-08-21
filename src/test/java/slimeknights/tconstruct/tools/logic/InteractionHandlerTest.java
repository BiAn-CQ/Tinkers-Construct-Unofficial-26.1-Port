package slimeknights.tconstruct.tools.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InteractionHandlerTest {
  @Test
  void cancelsBlockBreakAfterModifiableToolHandlesIt() {
    Player player = mock(Player.class);
    ItemStack stack = mock(ItemStack.class);
    ModifiableItem item = mock(ModifiableItem.class);
    BreakBlockEvent event = mock(BreakBlockEvent.class);
    BlockPos pos = BlockPos.ZERO;

    when(event.getPlayer()).thenReturn(player);
    when(event.getPos()).thenReturn(pos);
    when(player.getMainHandItem()).thenReturn(stack);
    when(stack.getItem()).thenReturn(item);
    when(item.onBlockStartBreak(stack, pos, player)).thenReturn(true);

    InteractionHandler.breakBlock(event);

    verify(event).setCanceled(true);
  }

  @Test
  void leavesBlockBreakUnchangedWhenModifiableToolDeclinesIt() {
    Player player = mock(Player.class);
    ItemStack stack = mock(ItemStack.class);
    ModifiableItem item = mock(ModifiableItem.class);
    BreakBlockEvent event = mock(BreakBlockEvent.class);
    BlockPos pos = BlockPos.ZERO;

    when(event.getPlayer()).thenReturn(player);
    when(event.getPos()).thenReturn(pos);
    when(player.getMainHandItem()).thenReturn(stack);
    when(stack.getItem()).thenReturn(item);
    when(item.onBlockStartBreak(stack, pos, player)).thenReturn(false);

    InteractionHandler.breakBlock(event);

    verify(event, never()).setCanceled(true);
  }
}
