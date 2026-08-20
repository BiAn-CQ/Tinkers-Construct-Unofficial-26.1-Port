package slimeknights.tconstruct.library.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;

/**
 * Server-safe entry points for client-only rendering updates.
 *
 * <p>The tank contents live in block-entity model data.  Updating the tank or
 * its light level alone does not invalidate the chunk mesh, so the client
 * must explicitly request model data and mark the block as changed.</p>
 */
public final class SafeClient {
  private SafeClient() {}

  /** Updates the world model when a tank crosses a rendered fluid increment. */
  public static void updateFluidModel(BlockEntity te, FluidTankAnimated tank, int oldAmount, int newAmount) {
    if (FMLEnvironment.getDist() == Dist.CLIENT) {
      ClientOnly.updateFluidModel(te, oldAmount, newAmount);
    }
  }

  /** Keeps the client-only Minecraft classes out of dedicated-server loading. */
  private static final class ClientOnly {
    private static void updateFluidModel(BlockEntity te, int oldAmount, int newAmount) {
      var level = te.getLevel();
      if (level != null && level.isClientSide() && oldAmount != newAmount) {
        te.requestModelDataUpdate();
        BlockState state = te.getBlockState();
        Minecraft.getInstance().levelRenderer.blockChanged(level, te.getBlockPos(), state, state, 3);
      }
    }
  }
}
