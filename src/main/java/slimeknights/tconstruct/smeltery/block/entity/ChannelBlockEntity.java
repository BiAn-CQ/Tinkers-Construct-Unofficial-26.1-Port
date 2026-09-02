package slimeknights.tconstruct.smeltery.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.EmptyResourceHandler;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.fluid.FillOnlyFluidHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.ChannelBlock;
import slimeknights.tconstruct.smeltery.block.ChannelBlock.ChannelConnection;
import slimeknights.tconstruct.smeltery.block.entity.tank.ChannelSideTank;
import slimeknights.tconstruct.smeltery.block.entity.tank.ChannelTank;
import slimeknights.tconstruct.smeltery.network.ChannelFlowPacket;
import slimeknights.tconstruct.smeltery.network.FluidUpdatePacket;
import slimeknights.tconstruct.smeltery.network.FluidUpdatePacket.IFluidPacketReceiver;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * Logic for channel fluid transfer
 */
public class ChannelBlockEntity extends MantleBlockEntity implements IFluidPacketReceiver {
	/** Channel internal tank */
	private final ChannelTank tank = new ChannelTank(FaucetBlockEntity.MB_PER_TICK * 4, this);
	/** Handler to return from channel top */
	private final ResourceHandler<FluidResource> topHandler = new FillOnlyFluidHandler(tank);
	/** Tanks for inserting on each side */
	private final Map<Direction,ResourceHandler<FluidResource>> sideTanks = createSideTanks();
	/** Cache of tanks on all neighboring sides */
	private final Map<Direction,NeighborCache> neighborTanks = new EnumMap<>(Direction.class);

  /** Ticker instance for this TE, serverside only */
  public static final BlockEntityTicker<ChannelBlockEntity> SERVER_TICKER = (level, pos, state, self) -> self.tick(state);

	/** Stores if the channel is currently flowing, set to 2 to allow a small buffer */
	private final byte[] isFlowing = new byte[5];

	public ChannelBlockEntity(BlockPos pos, BlockState state) {
		this(TinkerSmeltery.channel.get(), pos, state);
	}

	protected ChannelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/**
	 * Gets the central fluid tank of this channel
	 * @return  Central tank
	 */
	public FluidStack getFluid() {
		return this.tank.getFluid();
	}

	public AABB getRenderBoundingBox() {
		return new AABB(worldPosition.getX(), worldPosition.getY() - 1, worldPosition.getZ(), worldPosition.getX() + 1, worldPosition.getY() + 1, worldPosition.getZ() + 1);
	}

	/* Fluid handlers */

	/** Returns the handler exposed on the queried side. */
	@Nullable
	public ResourceHandler<FluidResource> getFluidHandler(@Nullable Direction side) {
		// top side gets the insert direct
    if (side == null || side == Direction.UP) {
      return topHandler;
    }
    // side tanks keep track of which side inserts
    if (side != Direction.DOWN) {
      ChannelConnection connection = getBlockState().getValue(ChannelBlock.DIRECTION_MAP.get(side));
      if (connection == ChannelConnection.IN) {
        return sideTanks.get(side);
      }
      // OUT deliberately advertises fluid support while rejecting all interaction.
      if (connection == ChannelConnection.OUT) {
        return EmptyResourceHandler.instance();
      }
    }
		return null;
	}

	private Map<Direction,ResourceHandler<FluidResource>> createSideTanks() {
		Map<Direction,ResourceHandler<FluidResource>> handlers = new EnumMap<>(Direction.class);
		for (Direction direction : Plane.HORIZONTAL) {
			handlers.put(direction, new ChannelSideTank(this, tank, direction));
		}
		return handlers;
	}

	/** Native neighbor cache. Its validity follows this block entity and the exact cache entry. */
	private final class NeighborCache {
		private final BlockCapabilityCache<ResourceHandler<FluidResource>,Direction> cache;

		private NeighborCache(ServerLevel level, Direction side) {
			cache = BlockCapabilityCache.create(
				Capabilities.Fluid.BLOCK, level, worldPosition.relative(side), side.getOpposite(),
				() -> !isRemoved() && neighborTanks.get(side) == this, () -> {});
		}

		@Nullable
		private ResourceHandler<FluidResource> get() {
			return cache.getCapability();
		}
	}

	/**
	 * Gets the fluid handler from a neighbor
	 * @param side  Side of the neighbor to fetch
	 * @return  Fluid handler, or empty
	 */
	@Nullable
	protected ResourceHandler<FluidResource> getNeighborHandler(Direction side) {
		assert level != null;
		if (level instanceof ServerLevel serverLevel) {
			return neighborTanks.computeIfAbsent(side, key -> new NeighborCache(serverLevel, key)).get();
		}
		return level.getCapability(Capabilities.Fluid.BLOCK, worldPosition.relative(side), side.getOpposite());
	}

	/**
	 * Removes a cached handler from the given neighbor as the block changed
	 * @param side  Side to remove
	 */
	public void removeCachedNeighbor(Direction side) {
		neighborTanks.remove(side);
	}

	/**
	 * Refreshes a neighbor based on the new connection
	 * @param state  The state that will later be put in the world, may not be the state currently in the world
	 * @param side   Side to update
	 */
	public void refreshNeighbor(BlockState state, Direction side) {
		// for below, only thing that needs to invalidate is if we are no longer connected down, remove the listener below
		if (side == Direction.DOWN) {
			if (!state.getValue(ChannelBlock.DOWN)) {
				neighborTanks.remove(Direction.DOWN);
			}
		} else if (side != Direction.UP) {
			ChannelConnection connection = state.getValue(ChannelBlock.DIRECTION_MAP.get(side));
			// if no longer flowing out, remove the neighbor tank
			if (connection != ChannelConnection.OUT) {
				neighborTanks.remove(side);
			}
			// The side-aware provider changed identity or presence; notify native caches.
			if (level != null) level.invalidateCapabilities(worldPosition);
		}
	}


	/* Flowing property */

	/**
	 * Gets the index for the given side for flowing. Same as regular index but without up
	 * @param side  Side to index
	 * @return Flow index
	 */
	private int getFlowIndex(Direction side) {
		if (side.getAxis().isVertical()) {
			return 0;
		}
		return side.get3DDataValue() - 1;
	}

	/**
	 * Marks the given side as flowing for the sake of rendering
	 * @param side     Side to set
	 * @param flowing  True to mark it as flowing
	 */
	public void setFlow(Direction side, boolean flowing) {
		if (side == Direction.UP) {
			return;
		}
		// update flowing state
		int index = getFlowIndex(side);
		boolean wasFlowing = isFlowing[index] > 0;
		isFlowing[index] = (byte)(flowing ? 2 : 0);

		// send packet to client if it changed
		if(wasFlowing != flowing && level != null && !level.isClientSide()) {
			syncFlowToClient(side, flowing);
		}
	}

	/**
	 * Checks if the given side is flowing
	 * @param side  Side to check
	 * @return  True if flowing
	 */
	public boolean isFlowing(Direction side) {
		if (side == Direction.UP) {
			return false;
		}

		return isFlowing[getFlowIndex(side)] > 0;
	}


	/* Utilities */

	/**
	 * Gets the connection for a side
	 * @param side  Side to query
	 * @return  Connection on the specified side
	 */
	protected boolean isOutput(Direction side) {
		// just always return in for up, thats fine
		if(side == Direction.UP) {
			return false;
		}
		// down is boolean, sides is multistate
		if(side == Direction.DOWN) {
			return this.getBlockState().getValue(ChannelBlock.DOWN);
		}
		return this.getBlockState().getValue(ChannelBlock.DIRECTION_MAP.get(side)) == ChannelConnection.OUT;
	}

	/**
	 * Counts the number of side outputs on the given side
	 * @param state  State to check
	 * @return  Number of outputs
	 */
	private static int countOutputs(BlockState state) {
		int count = 0;
		for (Direction direction : Plane.HORIZONTAL) {
			if (state.getValue(ChannelBlock.DIRECTION_MAP.get(direction)) == ChannelConnection.OUT) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Syncs the given flowing state to the client side
	 * @param side     Side to sync
	 * @param flowing  Flowing state to sync
	 */
	private void syncFlowToClient(Direction side, boolean flowing) {
		TinkerNetwork.getInstance().sendToClientsAround(new ChannelFlowPacket(worldPosition, side, flowing), level, worldPosition);
	}


	/* Flow */

	/**
	 * Server ticking logic
	 */
	private void tick(BlockState state) {
		// must have fluid first
		FluidStack fluid = tank.getFluid();
		if (!fluid.isEmpty()) {
			// if we have down and can flow, skip sides
			boolean hasFlown = false;
			if(state.getValue(ChannelBlock.DOWN)) {
				hasFlown = trySide(Direction.DOWN, FaucetBlockEntity.MB_PER_TICK);
			}
			// try sides if we have any sides
			int outputs = countOutputs(state);
			if(!hasFlown && outputs > 0) {
				// split the fluid evenly between sides
				int flowRate = Mth.clamp(tank.getMaxUsable() / outputs, 1, FaucetBlockEntity.MB_PER_TICK);
				// then transfer on each side
				for(Direction side : Plane.HORIZONTAL) {
					trySide(side, flowRate);
				}
			}
		}

		// clear flowing if we should no longer flow on a side
		for (int i = 0; i < 5; i++) {
			if (isFlowing[i] > 0) {
				isFlowing[i]--;
				if (isFlowing[i] == 0) {
					Direction direction;
					if (i == 0) {
						direction = Direction.DOWN;
					} else {
						direction = Direction.from3DDataValue(i + 1);
					}
					syncFlowToClient(direction, false);
				}
			}
		}

		tank.freeFluid();
	}

	/**
	 * Tries transferring fluid on a single side of the channel
	 * @param side      Side to transfer from
	 * @param flowRate  Maximum amount to output
	 * @return  True if the side transferred fluid
	 */
	protected boolean trySide(Direction side, int flowRate) {
		if(tank.isEmpty() || !this.isOutput(side)) {
			return false;
		}

		// get the handler on the side, try filling
    // TODO: handle the case of no fluid handler on the side that may later become a handler
		ResourceHandler<FluidResource> handler = getNeighborHandler(side);
		return handler != null && fill(side, handler, flowRate);
	}

	/**
	 * Fill the fluid handler on the given side
	 * @param side     Side to fill
	 * @param handler  Handler to fill
	 * @param amount   Amount to fill
	 * @return  True if the side successfully filled something
	 */
	protected boolean fill(Direction side, ResourceHandler<FluidResource> handler, int amount) {
		// make sure we do not allow more than the fluid allows, should not happen but just in case
		int usable = Math.min(tank.getMaxUsable(), amount);
		if (usable > 0) {
			FluidResource fluid = tank.getResource(0);
			int filled = 0;
			try (Transaction transaction = Transaction.open(null)) {
				int accepted = handler.insert(fluid, usable, transaction);
				if (accepted > 0 && tank.extract(0, fluid, accepted, transaction) == accepted) {
					filled = accepted;
					transaction.commit();
				}
			}
			if (filled > 0) {

				// mark that the side is flowing
				setFlow(side, true);
				return true;
			}
		}

		// failed to flow, mark side as not flowing
		setFlow(side, false);
		return false;
	}


	/* NBT and sync */
	private static final String TAG_IS_FLOWING = "is_flowing";
	private static final String TAG_TANK = "tank";

	/**
	 * Sends a fluid update to the client with the current fluid
	 */
	public void sendFluidUpdate() {
		if (level != null && !level.isClientSide()) {
			TinkerNetwork.getInstance().sendToClientsAround(new FluidUpdatePacket(worldPosition, getFluid()), level, worldPosition);
		}
	}

	@Override
  public void updateFluidTo(FluidStack fluid) {
		tank.setFluid(fluid);
	}

  @Override
  protected boolean shouldSyncOnUpdate() {
    return true;
  }

  @Override
  protected void saveSynced(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
    super.saveSynced(nbt, provider);
    nbt.putByteArray(TAG_IS_FLOWING, isFlowing);
    nbt.put(TAG_TANK, tank.writeToNBT(provider, new CompoundTag()));
  }

	@Override
  public void load(CompoundTag nbt) {
		super.load(nbt);

		// isFlowing
		if (nbt.contains(TAG_IS_FLOWING)) {
			byte[] nbtFlowing = nbt.getByteArray(TAG_IS_FLOWING).orElse(new byte[0]);
			int max = Math.min(5, nbtFlowing.length);
			for (int i = 0; i < max; i++) {
				byte b = nbtFlowing[i];
				if (b > 2) {
					isFlowing[i] = 2;
				} else if (b < 0) {
					isFlowing[i] = 0;
				} else {
					isFlowing[i] = b;
				}
			}
		}

		// tank
		CompoundTag tankTag = nbt.getCompoundOrEmpty(TAG_TANK);
		tank.readFromNBT(registries, tankTag);
	}
}
