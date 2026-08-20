package slimeknights.tconstruct.library.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;

/** Helpers related to Tag */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TagUtil {
  /* Helper functions */

  /**
   * Reads a block position from Tag
   * @param parent  Parent tag
   * @param key     Position key
   * @param offset  Amount to offset position by
   * @return  Block position, or null if invalid or missing
   */
  @Nullable
  public static BlockPos readOptionalPos(CompoundTag parent, String key, BlockPos offset) {
    BlockPos pos = readBlockPos(parent.get(key));
    if (pos != null) {
      return pos.offset(offset);
    }
    return null;
  }

  /** Reads both the legacy Tinkers compound format and the native 1.21 codec format. */
  @Nullable
  public static BlockPos readBlockPos(@Nullable Tag tag) {
    if (tag instanceof CompoundTag compound
        && compound.contains("X")
        && compound.contains("Y")
        && compound.contains("Z")) {
      return new BlockPos(compound.getIntOr("X", 0), compound.getIntOr("Y", 0), compound.getIntOr("Z", 0));
    }
    return tag == null ? null : BlockPos.CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(null);
  }

  /** Writes the established Tinkers compound format to preserve existing save data. */
  public static CompoundTag writeBlockPos(BlockPos pos) {
    CompoundTag tag = new CompoundTag();
    tag.putInt("X", pos.getX());
    tag.putInt("Y", pos.getY());
    tag.putInt("Z", pos.getZ());
    return tag;
  }

  /**
   * Checks if the given tag is a numeric type
   * @param tag  Tag to check
   * @return  True if the type matches
   */
  public static boolean isNumeric(Tag tag) {
    byte type = tag.getId();
    return type == Tag.TAG_BYTE || type == Tag.TAG_SHORT || type == Tag.TAG_INT || type == Tag.TAG_LONG || type == Tag.TAG_FLOAT || type == Tag.TAG_DOUBLE;
  }
}
