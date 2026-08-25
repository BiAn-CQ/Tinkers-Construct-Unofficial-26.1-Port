package slimeknights.tconstruct.smeltery.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.network.packet.BlockEntityPacket;
import slimeknights.tconstruct.smeltery.block.entity.ChannelBlockEntity;

/** Packet for when the flowing state changes on a channel side */
public class ChannelFlowPacket implements BlockEntityPacket<ChannelBlockEntity> {
	private final BlockPos pos;
	private final Direction side;
	private final boolean flow;
	public ChannelFlowPacket(BlockPos pos, Direction side, boolean flow) {
		this.pos = pos;
		this.side = side;
		this.flow = flow;
	}

	public ChannelFlowPacket(FriendlyByteBuf buffer) {
		pos = buffer.readBlockPos();
		side = buffer.readEnum(Direction.class);
		flow = buffer.readBoolean();
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeBlockPos(pos);
		buffer.writeEnum(side);
		buffer.writeBoolean(flow);
	}

	@Override
	public BlockPos pos() {
		return pos;
	}

	@Override
	public Class<ChannelBlockEntity> receiverType() {
		return ChannelBlockEntity.class;
	}

	@Override
	public void handleBlockEntity(IPayloadContext context, ChannelBlockEntity blockEntity) {
		blockEntity.setFlow(side, flow);
	}
}
