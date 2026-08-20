package slimeknights.tconstruct.tables.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity;
import slimeknights.tconstruct.library.utils.TinkerNetworkBuffer;


/**
 * Packet to send the current crafting recipe to a player who opens the tinker station
 */
public class UpdateTinkerStationRecipePacket implements IThreadsafePacket {
  private final BlockPos pos;
  private final RecipeHolder<ITinkerStationRecipe> recipe;

  public UpdateTinkerStationRecipePacket(BlockPos pos, RecipeHolder<ITinkerStationRecipe> recipe) {
    this.pos = pos;
    this.recipe = recipe;
  }

  public UpdateTinkerStationRecipePacket(FriendlyByteBuf buffer) {
    this.pos = buffer.readBlockPos();
    RecipeHolder<?> decoded = RecipeHolder.STREAM_CODEC.decode(TinkerNetworkBuffer.registry(buffer));
    if (!(decoded.value() instanceof ITinkerStationRecipe stationRecipe)) {
      throw new IllegalArgumentException("Expected a Tinker Station recipe, got " + decoded.value().getClass().getName());
    }
    this.recipe = new RecipeHolder<>(decoded.id(), stationRecipe);
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeBlockPos(pos);
    RecipeHolder.STREAM_CODEC.encode(TinkerNetworkBuffer.registry(buffer), recipe);
  }

  @Override
  public void handleThreadsafe(IPayloadContext context) {
    HandleClient.handle(this);
  }

  /** Safely runs client side only code in a method only called on client */
  private static class HandleClient {
    private static void handle(UpdateTinkerStationRecipePacket packet) {
      Level world = Minecraft.getInstance().level;
      if (world != null) {
        BlockEntityHelper.get(TinkerStationBlockEntity.class, world, packet.pos)
          .ifPresent(te -> te.updateRecipe(packet.recipe));
      }
    }
  }
}
