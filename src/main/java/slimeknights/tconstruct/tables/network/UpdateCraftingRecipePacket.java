package slimeknights.tconstruct.tables.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.tconstruct.common.network.BlockEntityPacket;
import slimeknights.tconstruct.tables.block.entity.table.CraftingStationBlockEntity;
import slimeknights.tconstruct.library.utils.TinkerNetworkBuffer;

/**
 * Packet to send the current crafting recipe to a player who opens the crafting station
 */
public class UpdateCraftingRecipePacket implements BlockEntityPacket<CraftingStationBlockEntity> {
  private final BlockPos pos;
  private final RecipeHolder<CraftingRecipe> recipe;

  public UpdateCraftingRecipePacket(BlockPos pos, RecipeHolder<CraftingRecipe> recipe) {
    this.pos = pos;
    this.recipe = recipe;
  }

  public UpdateCraftingRecipePacket(FriendlyByteBuf buffer) {
    this.pos = buffer.readBlockPos();
    RecipeHolder<?> decoded = RecipeHolder.STREAM_CODEC.decode(TinkerNetworkBuffer.registry(buffer));
    if (!(decoded.value() instanceof CraftingRecipe craftingRecipe)) {
      throw new IllegalArgumentException("Expected a crafting recipe, got " + decoded.value().getClass().getName());
    }
    this.recipe = new RecipeHolder<>(decoded.id(), craftingRecipe);
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeBlockPos(pos);
    RecipeHolder.STREAM_CODEC.encode(TinkerNetworkBuffer.registry(buffer), recipe);
  }

  @Override
  public BlockPos pos() {
    return pos;
  }

  @Override
  public Class<CraftingStationBlockEntity> receiverType() {
    return CraftingStationBlockEntity.class;
  }

  @Override
  public void handleBlockEntity(IPayloadContext context, CraftingStationBlockEntity blockEntity) {
    blockEntity.updateRecipe(recipe);
  }
}
