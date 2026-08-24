package slimeknights.tconstruct.smeltery.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.tconstruct.common.network.BlockEntityPacket;
import slimeknights.tconstruct.library.utils.TinkerNetworkBuffer;
import slimeknights.tconstruct.smeltery.block.entity.tank.ISmelteryTankHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet sent whenever the contents of the smeltery tank change
 */
public class SmelteryTankUpdatePacket implements BlockEntityPacket<ISmelteryTankHandler> {
  private final BlockPos pos;
  private final List<FluidStack> fluids;

  public SmelteryTankUpdatePacket(BlockPos pos, List<FluidStack> fluids) {
    this.pos = pos.immutable();
    // The smeltery mutates both this list and its stacks while packets are in flight.
    // Snapshot each entry so the client receives one coherent tank state.
    this.fluids = fluids.stream().map(FluidStack::copy).toList();
  }

  public SmelteryTankUpdatePacket(FriendlyByteBuf buffer) {
    pos = buffer.readBlockPos();
    int size = buffer.readVarInt();
    fluids = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      fluids.add(FluidStack.OPTIONAL_STREAM_CODEC.decode(TinkerNetworkBuffer.registry(buffer)));
    }
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeBlockPos(pos);
    buffer.writeVarInt(fluids.size());
    for (FluidStack fluid : fluids) {
      FluidStack.OPTIONAL_STREAM_CODEC.encode(TinkerNetworkBuffer.registry(buffer), fluid);
    }
  }

  @Override
  public BlockPos pos() {
    return pos;
  }

  @Override
  public Class<ISmelteryTankHandler> receiverType() {
    return ISmelteryTankHandler.class;
  }

  @Override
  public void handleBlockEntity(IPayloadContext context, ISmelteryTankHandler blockEntity) {
    blockEntity.updateFluidsFromPacket(fluids);
  }
}
