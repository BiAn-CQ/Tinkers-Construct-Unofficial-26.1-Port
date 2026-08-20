package slimeknights.tconstruct.tables.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.material.IMaterialValue;
import slimeknights.tconstruct.library.recipe.partbuilder.IPartBuilderRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.utils.TinkerNetworkBuffer;
import slimeknights.tconstruct.tables.block.entity.table.PartBuilderBlockEntity;
import slimeknights.tconstruct.tables.client.inventory.BaseTabbedScreen;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Sends the part builder's server-filtered recipe list to the client.
 *
 * <p>26.1 no longer exposes the server recipe manager through a client level.
 * Part builder buttons are input-sensitive, so syncing only a recipe ID is not
 * enough: the pattern associated with each recipe and the current material
 * value are also part of the display state.</p>
 */
public final class UpdatePartBuilderRecipesPacket implements IThreadsafePacket {
  /** One visible pattern and the recipe that handles it. */
  public record RecipeEntry(Pattern pattern, RecipeHolder<IPartBuilderRecipe> recipe) {}

  /** Material information needed to render and validate the client preview. */
  public record MaterialData(MaterialVariantId material, int value, int needed, ItemStack leftover) {
    public MaterialData {
      leftover = leftover.copy();
    }

    @Nullable
    private static MaterialData from(@Nullable IMaterialValue value) {
      if (value == null || value.getMaterial().getVariant().equals(IMaterial.UNKNOWN_ID)) {
        return null;
      }
      return new MaterialData(value.getMaterial().getVariant(), value.getValue(), value.getNeeded(), value.getLeftover());
    }
  }

  private final BlockPos pos;
  private final List<RecipeEntry> recipes;
  @Nullable
  private final MaterialData material;

  /** Creates a packet from the authoritative server-side display state. */
  public UpdatePartBuilderRecipesPacket(BlockPos pos, List<RecipeEntry> recipes, @Nullable IMaterialValue material) {
    this(pos, List.copyOf(recipes), MaterialData.from(material));
  }

  private UpdatePartBuilderRecipesPacket(BlockPos pos, List<RecipeEntry> recipes, @Nullable MaterialData material) {
    this.pos = pos;
    this.recipes = recipes;
    this.material = material;
  }

  public UpdatePartBuilderRecipesPacket(FriendlyByteBuf buffer) {
    this.pos = buffer.readBlockPos();
    int size = buffer.readVarInt();
    if (size < 0 || size > 4096) {
      throw new IllegalArgumentException("Invalid part builder recipe count: " + size);
    }
    this.recipes = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      Pattern pattern = new Pattern(buffer.readIdentifier());
      RecipeHolder<?> decoded = RecipeHolder.STREAM_CODEC.decode(TinkerNetworkBuffer.registry(buffer));
      if (!(decoded.value() instanceof IPartBuilderRecipe recipe)) {
        throw new IllegalArgumentException("Expected a part builder recipe, got " + decoded.value().getClass().getName());
      }
      this.recipes.add(new RecipeEntry(pattern, new RecipeHolder<>(decoded.id(), recipe)));
    }

    if (buffer.readBoolean()) {
      MaterialVariantId materialId = MaterialVariantId.parse(buffer.readUtf(Short.MAX_VALUE));
      int value = buffer.readVarInt();
      int needed = buffer.readVarInt();
      ItemStack leftover = ItemStack.OPTIONAL_STREAM_CODEC.decode(TinkerNetworkBuffer.registry(buffer));
      this.material = new MaterialData(materialId, value, needed, leftover);
    } else {
      this.material = null;
    }
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeBlockPos(pos);
    buffer.writeVarInt(recipes.size());
    for (RecipeEntry entry : recipes) {
      buffer.writeIdentifier(entry.pattern().location());
      RecipeHolder.STREAM_CODEC.encode(TinkerNetworkBuffer.registry(buffer), entry.recipe());
    }

    buffer.writeBoolean(material != null);
    if (material != null) {
      buffer.writeUtf(material.material().toString());
      buffer.writeVarInt(material.value());
      buffer.writeVarInt(material.needed());
      ItemStack.OPTIONAL_STREAM_CODEC.encode(TinkerNetworkBuffer.registry(buffer), material.leftover());
    }
  }

  @Override
  public void handleThreadsafe(IPayloadContext context) {
    HandleClient.handle(this);
  }

  /** Safely runs client-only block entity code from the common packet class. */
  private static class HandleClient {
    private static void handle(UpdatePartBuilderRecipesPacket packet) {
      Level world = Minecraft.getInstance().level;
      if (world != null) {
        BlockEntityHelper.get(PartBuilderBlockEntity.class, world, packet.pos)
          .ifPresent(te -> {
            te.updateRecipes(packet.recipes, packet.material);
            Screen screen = Minecraft.getInstance().screen;
            if (screen instanceof BaseTabbedScreen<?,?> tabbedScreen) {
              tabbedScreen.updateDisplay();
            }
          });
      }
    }
  }
}
