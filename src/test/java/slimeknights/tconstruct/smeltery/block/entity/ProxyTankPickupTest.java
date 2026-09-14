package slimeknights.tconstruct.smeltery.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.tconstruct.common.network.InventorySlotSyncPacket;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.test.CoreTestBootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProxyTankPickupTest extends CoreTestBootstrap {
  @Test
  void occupiedHandPickupSendsRemovalUpdate() {
    BlockEntityType<?> type = mock(BlockEntityType.class);
    when(type.isValid(any())).thenReturn(true);
    ProxyTankBlockEntity proxy = new ProxyTankBlockEntity(type, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
    proxy.getItemTank().setStack(new ItemStack(Items.BUCKET));
    Level level = mock(Level.class);
    proxy.setLevel(level);
    Player player = mock(Player.class);
    when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(new ItemStack(Items.DIRT));
    when(player.addItem(any())).thenAnswer(invocation -> {
      ItemStack moved = invocation.getArgument(0);
      assertThat(moved).isNotSameAs(proxy.getItemTank().getStack());
      moved.setCount(0);
      return true;
    });
    TinkerNetwork network = mock(TinkerNetwork.class);
    try (var networks = mockStatic(TinkerNetwork.class);
         var fluids = mockStatic(FluidTransferHelper.class)) {
      networks.when(TinkerNetwork::getInstance).thenReturn(network);
      fluids.when(() -> FluidTransferHelper.interactWithContainer(any(), any(), any(), any(Player.class), any(InteractionHand.class)))
        .thenReturn(FluidTransferHelper.FluidInteractionResult.MISSING);
      fluids.when(() -> FluidTransferHelper.interactWithFilledBucket(any(), any(), any(), any(), any(), any()))
        .thenReturn(FluidTransferHelper.FluidInteractionResult.MISSING);

      proxy.interact(player, InteractionHand.MAIN_HAND, false);

      assertThat(proxy.getItemTank().getStack().isEmpty()).isTrue();
      verify(network).sendToClientsAround(any(InventorySlotSyncPacket.class), eq(level), eq(BlockPos.ZERO));
    }
  }
}
