package slimeknights.tconstruct.tables.network;

import lombok.RequiredArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.tables.block.ITabbedBlock;
import slimeknights.tconstruct.tables.menu.TabbedContainerMenu;

@RequiredArgsConstructor
public class StationTabPacket implements IThreadsafePacket {
  private final BlockPos pos;

  public StationTabPacket(FriendlyByteBuf buffer) {
    this.pos = buffer.readBlockPos();
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeBlockPos(pos);
  }

  @Override
  public void handleThreadsafe(IPayloadContext context) {
    if (context.player() instanceof ServerPlayer sender) {
      Level world = sender.level();
      if (!world.hasChunkAt(pos)) {
        return;
      }
      BlockState state = world.getBlockState(pos);
      // Only allow tabs advertised by the currently open tabbed menu. Besides preventing
      // arbitrary menu opening, this keeps an invalid client packet from consuming its cursor stack.
      if (!(sender.containerMenu instanceof TabbedContainerMenu<?> menu)
          || menu.stationBlocks.stream().noneMatch(pair -> pair.getLeft().equals(pos))
          || !(state.getBlock() instanceof ITabbedBlock tabbed)) {
        return;
      }
      ItemStack heldStack = sender.containerMenu.getCarried();
      if (!heldStack.isEmpty()) {
        sender.containerMenu.setCarried(ItemStack.EMPTY);
      }
      {
        // Close only the server menu. A client close packet briefly grabs the mouse
        // before the replacement screen opens, which recenters the cursor.
        sender.doCloseContainer();
        tabbed.openGui(sender, world, pos);
      }

      if (!heldStack.isEmpty()) {
        sender.containerMenu.setCarried(heldStack);
        TinkerNetwork.getInstance().sendVanillaPacket(sender, new ClientboundContainerSetSlotPacket(-1, -1, -1, heldStack));
      }
    }
  }
}
