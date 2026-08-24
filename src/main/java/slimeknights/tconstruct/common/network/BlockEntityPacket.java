package slimeknights.tconstruct.common.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.TConstruct;

import javax.annotation.Nullable;

/**
 * Helper for clientbound packets that update a block entity.
 * @param <T> receiver type
 */
public interface BlockEntityPacket<T> extends IThreadsafePacket {
  /** Gets the target block position. */
  BlockPos pos();

  /** Gets the expected receiver type. */
  Class<T> receiverType();

  @Override
  default void handleThreadsafe(IPayloadContext context) {
    BlockPos pos = pos();
    BlockEntity blockEntity = getBlockEntity(pos, this);
    if (blockEntity != null) {
      Class<T> type = receiverType();
      if (type.isInstance(blockEntity)) {
        handleBlockEntity(context, type.cast(blockEntity));
      } else {
        TConstruct.LOG.error("Failed to handle packet {}: block entity type mismatch at {}, expected {}, found {}",
          this, pos, type, blockEntity.getClass());
      }
    } else {
      TConstruct.LOG.error("Failed to handle packet {}: no block entity at {}", this, pos);
    }
  }

  /** Handles the block entity after its position and type have been validated. */
  void handleBlockEntity(IPayloadContext context, T blockEntity);

  /** Gets a loaded block entity, or {@code null} when the level or chunk is unavailable. */
  @Nullable
  static BlockEntity getBlockEntity(@Nullable BlockGetter level, BlockPos pos, Object packet) {
    if (BlockEntityHelper.isBlockLoaded(level, pos)) {
      return level.getBlockEntity(pos);
    }
    TConstruct.LOG.error("Failed to handle packet {}: level is not loaded at {}", packet, pos);
    return null;
  }

  /** Client-only overload using the active client level. */
  @Nullable
  static BlockEntity getBlockEntity(BlockPos pos, Object packet) {
    return getBlockEntity(SafeClientAccess.getLevel(), pos, packet);
  }
}
