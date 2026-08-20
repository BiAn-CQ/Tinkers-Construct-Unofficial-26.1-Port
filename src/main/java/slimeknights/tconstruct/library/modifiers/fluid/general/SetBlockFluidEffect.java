package slimeknights.tconstruct.library.modifiers.fluid.general;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.data.loadable.common.BlockStateLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.tconstruct.library.modifiers.fluid.EffectLevel;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.modifiers.fluid.block.BreakBlockFluidEffect;

/**
 * Replaces a block with a different block using a fluid. Unlike {@link BreakBlockFluidEffect}, does not produce block drops or validate the block placememt.
 */
public final class SetBlockFluidEffect implements FluidEffect<FluidEffectContext> {
  public static final RecordLoadable<SetBlockFluidEffect> LOADER = new RecordLoadable<>() {
    @Override
    public SetBlockFluidEffect deserialize(JsonObject json, TypedMap context) {
      return new SetBlockFluidEffect(BlockStateLoadable.DIFFERENCE.getIfPresent(json, "block", context));
    }

    @Override
    public void serialize(SetBlockFluidEffect effect, JsonObject json) {
      if (effect.block != null) {
        json.add("block", BlockStateLoadable.DIFFERENCE.serialize(effect.block));
      } else {
        json.addProperty("block", effect.blockKey.identifier().toString());
      }
    }

    @Override
    public SetBlockFluidEffect decode(FriendlyByteBuf buffer, TypedMap context) {
      return new SetBlockFluidEffect(BlockStateLoadable.DIFFERENCE.decode(buffer, context));
    }

    @Override
    public void encode(FriendlyByteBuf buffer, SetBlockFluidEffect effect) {
      BlockStateLoadable.DIFFERENCE.encode(buffer, effect.block());
    }
  };
  public static final SetBlockFluidEffect AIR = new SetBlockFluidEffect(Blocks.AIR);

  private final ResourceKey<Block> blockKey;
  private final BlockState block;

  public SetBlockFluidEffect(ResourceKey<Block> block) {
    this.blockKey = block;
    this.block = null;
  }

  public SetBlockFluidEffect(Identifier block) {
    this(ResourceKey.create(Registries.BLOCK, block));
  }

  public SetBlockFluidEffect(BlockState block) {
    this.blockKey = BuiltInRegistries.BLOCK.getResourceKey(block.getBlock())
      .orElseThrow(() -> new IllegalArgumentException("Unregistered block"));
    this.block = block;
  }

  public SetBlockFluidEffect(Block block) {
    this(block.defaultBlockState());
  }

  public ResourceKey<Block> blockKey() {
    return blockKey;
  }

  public BlockState block() {
    return block != null ? block : BuiltInRegistries.BLOCK.get(blockKey.identifier())
      .orElseThrow(() -> new IllegalStateException("Unknown block " + blockKey.identifier())).value().defaultBlockState();
  }

  @Override
  public RecordLoadable<SetBlockFluidEffect> getLoader() {
    return LOADER;
  }

  @Override
  public float apply(FluidStack fluid, EffectLevel level, FluidEffectContext context, FluidAction action) {
    if (!level.isFull()) {
      return 0;
    }
    // find what was there before
    Level world = context.getLevel();
    BlockPos target = context.getBlockPos();
    BlockState original = world.getBlockState(target);
    BlockState replacement = block();
    if (original != replacement && !context.breakRestricted()) {
      if (action.execute() && !world.isClientSide()) {
        if (world.setBlockAndUpdate(target, replacement) && !original.isAir()) {
          world.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, target, Block.getId(original));
        }
      }
      return 1;
    }
    return 0;
  }

  @Override
  public Component getDescription(RegistryAccess registryAccess) {
    return FluidEffect.makeTranslation(getLoader(), Component.translatable(block().getBlock().getDescriptionId()));
  }
}
