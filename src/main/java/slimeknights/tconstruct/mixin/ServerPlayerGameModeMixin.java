package slimeknights.tconstruct.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.tconstruct.library.tools.helper.PrimaryBlockBreakHook;
import slimeknights.tconstruct.library.tools.helper.PrimaryBlockBreakContext;

/** Restores the primary modifiable-item hook in the server block-breaking pipeline. */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
  @Shadow @Final protected ServerPlayer player;
  @Unique private final PrimaryBlockBreakContext tconstruct$primaryBreakContext = new PrimaryBlockBreakContext();

  /** Marks the one destroyBlock call made by Minecraft in response to the player's mining packet. */
  @WrapOperation(
    method = "destroyAndAck",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/server/level/ServerPlayerGameMode;destroyBlock(Lnet/minecraft/core/BlockPos;)Z"
    )
  )
  private boolean tconstruct$markPlayerDestroy(ServerPlayerGameMode instance, BlockPos pos, Operation<Boolean> original) {
    int previousPlayerDepth = tconstruct$primaryBreakContext.enterPlayerAction();
    try {
      return original.call(instance, pos);
    } finally {
      tconstruct$primaryBreakContext.exitPlayerAction(previousPlayerDepth);
    }
  }

  /** Tracks nested and programmatic destroyBlock calls without depending on any chain-mining mod. */
  @WrapMethod(method = "destroyBlock")
  private boolean tconstruct$trackDestroyDepth(BlockPos pos, Operation<Boolean> original) {
    tconstruct$primaryBreakContext.enterDestroyBlock();
    try {
      return original.call(pos);
    } finally {
      tconstruct$primaryBreakContext.exitDestroyBlock();
    }
  }

  /**
   * Runs after NeoForge's break event and vanilla permission checks, but before vanilla mutates the block.
   * Keeping this hook in the player break pipeline means events fired to validate Tinkers' AOE blocks cannot
   * recursively start another primary harvest.
   */
  @Inject(
    method = "destroyBlock",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/world/level/block/Block;playerWillDestroy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/level/block/state/BlockState;",
      shift = At.Shift.BEFORE
    ),
    cancellable = true
  )
  private void tconstruct$onBlockStartBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
    if (tconstruct$primaryBreakContext.isPlayerAction() && PrimaryBlockBreakHook.handle(player, pos)) {
      cir.setReturnValue(false);
    }
  }
}
