package slimeknights.tconstruct.smeltery.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.EmptyResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

/** This class exists simply to allow us to have a block entity renderer for obsidian gauges. Though it is useful as a cache for the capability to render. */
public class GaugeBlockEntity extends BlockEntity {
  public GaugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  public GaugeBlockEntity(BlockPos pos, BlockState state) {
    this(TinkerSmeltery.gauge.get(), pos, state);
  }

  /** Gets the neighbor fluid handler. Used mainly for rendering client side */
  public ResourceHandler<FluidResource> getTank() {
    if (level == null) {
      return EmptyResourceHandler.instance();
    }
    Direction side = getBlockState().getValue(BlockStateProperties.FACING);
    ResourceHandler<FluidResource> handler = level.getCapability(Capabilities.Fluid.BLOCK, getBlockPos().relative(side.getOpposite()), side);
    return handler == null ? EmptyResourceHandler.instance() : handler;
  }
}
