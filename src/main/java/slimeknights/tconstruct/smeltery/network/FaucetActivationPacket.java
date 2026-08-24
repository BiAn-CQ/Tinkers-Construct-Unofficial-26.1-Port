package slimeknights.tconstruct.smeltery.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.tconstruct.common.network.BlockEntityPacket;
import slimeknights.tconstruct.library.utils.TinkerNetworkBuffer;
import slimeknights.tconstruct.smeltery.block.entity.FaucetBlockEntity;

/** Sent to clients to activate the faucet animation clientside **/
public class FaucetActivationPacket implements BlockEntityPacket<FaucetBlockEntity> {

  private final BlockPos pos;
  private final FluidStack fluid;
  private final boolean isPouring;
  public FaucetActivationPacket(BlockPos pos, FluidStack fluid, boolean isPouring) {
    this.pos = pos.immutable();
    this.fluid = fluid.copy();
    this.isPouring = isPouring;
  }

  public FaucetActivationPacket(FriendlyByteBuf buffer) {
    this.pos = buffer.readBlockPos();
    this.fluid = FluidStack.OPTIONAL_STREAM_CODEC.decode(TinkerNetworkBuffer.registry(buffer));
    this.isPouring = buffer.readBoolean();
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeBlockPos(pos);
    FluidStack.OPTIONAL_STREAM_CODEC.encode(TinkerNetworkBuffer.registry(buffer), fluid);
    buffer.writeBoolean(isPouring);
  }

  @Override
  public BlockPos pos() {
    return pos;
  }

  @Override
  public Class<FaucetBlockEntity> receiverType() {
    return FaucetBlockEntity.class;
  }

  @Override
  public void handleBlockEntity(IPayloadContext context, FaucetBlockEntity blockEntity) {
    blockEntity.onActivationPacket(fluid, isPouring);
  }
}
