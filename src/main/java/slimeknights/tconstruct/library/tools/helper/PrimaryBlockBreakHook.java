package slimeknights.tconstruct.library.tools.helper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.tools.item.IModifiable;

/** Restored primary block-break item hook for modifiable tools. */
public final class PrimaryBlockBreakHook {
  private PrimaryBlockBreakHook() {}

  /** Gives the held modifiable tool ownership of a server-initiated primary block break. */
  public static boolean handle(ServerPlayer player, BlockPos pos) {
    ItemStack stack = player.getMainHandItem();
    return stack.getItem() instanceof IModifiable modifiable
      && modifiable.onBlockStartBreak(stack, pos, player);
  }
}
