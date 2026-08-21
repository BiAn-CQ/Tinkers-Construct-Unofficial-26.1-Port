package slimeknights.tconstruct.plugin.ftbultimine;

import dev.ftb.mods.ftbultimine.api.blockbreaking.BlockBreakHandler;
import dev.ftb.mods.ftbultimine.api.neoforge.FTBUltimineEvent;
import dev.ftb.mods.ftbultimine.api.shape.Shape;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.NeoForge;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableLauncherItem;

/** Optional FTB Ultimine integration for tools that own their block-break flow. */
public final class FTBUltiminePlugin {
  private FTBUltiminePlugin() {}

  /** Called during mod construction after confirming FTB Ultimine is loaded. */
  public static void onConstruct() {
    NeoForge.EVENT_BUS.addListener(FTBUltiminePlugin::registerBlockBreakHandler);
  }

  private static void registerBlockBreakHandler(FTBUltimineEvent.RegisterBlockBreakHandler event) {
    event.getEventData().consumer().accept(FTBUltiminePlugin::breakTinkerBlock);
    TConstruct.LOG.info("Registered FTB Ultimine block-break compatibility");
  }

  private static BlockBreakHandler.Result breakTinkerBlock(Player player, BlockPos pos, BlockState state,
                                                             Shape shape, BlockHitResult hitResult) {
    if (!(player instanceof ServerPlayer serverPlayer) || !ownsBlockBreak(player.getMainHandItem().getItem())) {
      return BlockBreakHandler.Result.PASS;
    }

    BlockState before = serverPlayer.level().getBlockState(pos);
    boolean reportedSuccess = serverPlayer.gameMode.destroyBlock(pos);
    boolean blockChanged = !serverPlayer.level().getBlockState(pos).equals(before);
    return reportedSuccess || blockChanged ? BlockBreakHandler.Result.SUCCESS : BlockBreakHandler.Result.FAIL;
  }

  static boolean ownsBlockBreak(Item item) {
    return item instanceof ModifiableItem || item instanceof ModifiableLauncherItem;
  }
}
