package slimeknights.tconstruct.library.utils;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Codecs used by the 26.1 ValueInput/ValueOutput entity persistence API. */
public final class TinkerValueCodecs {
  public static final Codec<ItemStack> ITEM_STACK = ItemStack.CODEC;
  public static final Codec<BlockPos> BLOCK_POS = BlockPos.CODEC;
  public static final Codec<CompoundTag> COMPOUND = CompoundTag.CODEC;
  public static final Codec<java.util.List<CompoundTag>> COMPOUND_LIST = CompoundTag.CODEC.listOf();

  private TinkerValueCodecs() {}
}
