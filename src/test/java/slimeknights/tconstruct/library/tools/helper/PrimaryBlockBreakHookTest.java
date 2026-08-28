package slimeknights.tconstruct.library.tools.helper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrimaryBlockBreakHookTest {
  @Test
  void handlesPrimaryBreakForModifiableTool() {
    ServerPlayer player = mock(ServerPlayer.class);
    ItemStack stack = mock(ItemStack.class);
    ModifiableItem item = mock(ModifiableItem.class);
    BlockPos pos = BlockPos.ZERO;

    when(player.getMainHandItem()).thenReturn(stack);
    when(stack.getItem()).thenReturn(item);
    when(item.onBlockStartBreak(stack, pos, player)).thenReturn(true);

    assertThat(PrimaryBlockBreakHook.handle(player, pos)).isTrue();
    verify(item).onBlockStartBreak(stack, pos, player);
  }

  @Test
  void preservesVanillaBreakWhenModifiableToolDeclines() {
    ServerPlayer player = mock(ServerPlayer.class);
    ItemStack stack = mock(ItemStack.class);
    ModifiableItem item = mock(ModifiableItem.class);
    BlockPos pos = BlockPos.ZERO;

    when(player.getMainHandItem()).thenReturn(stack);
    when(stack.getItem()).thenReturn(item);

    assertThat(PrimaryBlockBreakHook.handle(player, pos)).isFalse();
    verify(item).onBlockStartBreak(stack, pos, player);
  }

  @Test
  void ignoresItemsThatDoNotOwnTheHarvestFlow() {
    ServerPlayer player = mock(ServerPlayer.class);
    ItemStack stack = mock(ItemStack.class);

    when(player.getMainHandItem()).thenReturn(stack);
    when(stack.getItem()).thenReturn(mock(Item.class));

    assertThat(PrimaryBlockBreakHook.handle(player, BlockPos.ZERO)).isFalse();
  }
}
