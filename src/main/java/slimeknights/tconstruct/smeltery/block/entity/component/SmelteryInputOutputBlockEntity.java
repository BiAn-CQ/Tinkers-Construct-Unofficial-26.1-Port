package slimeknights.tconstruct.smeltery.block.entity.component;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import slimeknights.mantle.block.entity.IRetexturedBlockEntity;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.common.multiblock.IMasterLogic;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.tank.ISmelteryTankHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

import static slimeknights.mantle.util.RetexturedHelper.TAG_TEXTURE;

/**
 * Shared logic between drains and ducts
 */
public abstract class SmelteryInputOutputBlockEntity<T> extends SmelteryComponentBlockEntity implements IRetexturedBlockEntity {
  /** Capability queried at the master position. The parent is always queried without a side. */
  private final BlockCapability<T,Direction> parentCapability;
  @Nullable
  private BlockCapabilityCache<T,Direction> capabilityCache;
  @Nullable
  private BlockPos cachedMaster;
  @Nullable
  private T cachedSource;
  @Nullable
  private T cachedHandler;

  /* Retexturing */
  @Nonnull
  @Getter
  private Block texture = Blocks.AIR;

  protected SmelteryInputOutputBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                           BlockCapability<T,Direction> parentCapability) {
    super(type, pos, state);
    this.parentCapability = parentCapability;
  }

  /** Clears all cached capabilities */
  private void clearHandler() {
    capabilityCache = null;
    cachedMaster = null;
    cachedSource = null;
    cachedHandler = null;
  }

  @Override
  public void onMasterLoad(IMasterLogic master) {
    clearHandler();
    // The exposed capability changes when the controller block entity is recreated,
    // including when it is placed back at the same position. Notify neighboring
    // capability caches such as faucets so they query this I/O block again.
    if (level != null) {
      level.invalidateCapabilities(worldPosition);
    }
  }

  @Override
  protected void setMaster(@Nullable BlockPos master, @Nullable Block block) {
    assert level != null;

    // if we have a new master, invalidate handlers
    boolean masterChanged = false;
    if (!Objects.equals(getMasterPos(), master)) {
      clearHandler();
      masterChanged = true;
    }
    super.setMaster(master, block);
    // notify neighbors of the change (state change skips the notify flag)
    if (masterChanged) {
      level.invalidateCapabilities(worldPosition);
      BlockState state = getBlockState();
      level.sendBlockUpdated(worldPosition, state, state, 3);
    }
  }

  /**
   * Gets the handler directly from the master block entity when the legacy controller API exposes it.
   * This preserves the original Tinkers behavior for Smeltery drains while still allowing generic
   * NeoForge capabilities for item I/O blocks.
   */
  @Nullable
  protected T getDirectMasterCapability(BlockEntity master) {
    return null;
  }

  /** Wraps the master's handler for the public capability exposed by this I/O block. */
  protected T wrapCapability(T capability) {
    return capability;
  }

  private void parentCapabilityInvalidated(ServerLevel serverLevel, BlockPos master) {
    cachedSource = null;
    cachedHandler = null;
    serverLevel.getServer().execute(() -> {
      if (!isRemoved() && Objects.equals(getMasterPos(), master)) {
        serverLevel.invalidateCapabilities(worldPosition);
      }
    });
  }

  /** Returns the master's capability, preserving the original null-side parent query and wrapper cache. */
  @Nullable
  public T getProxiedCapability(@Nullable Direction facing) {
    if (!validateMaster() || level == null) {
      clearHandler();
      return null;
    }
    BlockPos master = getMasterPos();
    if (master == null) {
      clearHandler();
      return null;
    }

    // Smeltery controllers intentionally do not expose the standard external fluid capability,
    // so their internal handler is queried directly before trying the public capability.
    BlockEntity masterBlockEntity = level.getBlockEntity(master);
    T source = masterBlockEntity == null ? null : getDirectMasterCapability(masterBlockEntity);
    if (source == null) {
      if (level instanceof ServerLevel serverLevel) {
        if (capabilityCache == null || !master.equals(cachedMaster)) {
          clearHandler();
          cachedMaster = master.immutable();
          BlockPos target = cachedMaster;
          capabilityCache = BlockCapabilityCache.create(
            parentCapability, serverLevel, target, null,
            () -> !isRemoved() && Objects.equals(getMasterPos(), target),
            () -> parentCapabilityInvalidated(serverLevel, target));
        }
        source = capabilityCache.getCapability();
      } else {
        source = level.getCapability(parentCapability, master, null);
      }
    }

    if (source == null) {
      cachedSource = null;
      cachedHandler = null;
      return null;
    }
    if (source != cachedSource) {
      cachedSource = source;
      cachedHandler = wrapCapability(source);
    }
    return cachedHandler;
  }


  /* Retexturing */

  @Override
  @Nonnull
  public ModelData getModelData() {
    return RetexturedHelper.getModelData(getTexture());
  }

  @Override
  public String getTextureName() {
    return RetexturedHelper.getTextureName(texture);
  }

  @Override
  public void updateTexture(String name) {
    Block oldTexture = texture;
    texture = RetexturedHelper.getBlock(name);
    if (oldTexture != texture) {
      setChangedFast();
      RetexturedHelper.onTextureUpdated(this);
    }
  }


  /* NBT */

  @Override
  protected boolean shouldSyncOnUpdate() {
    return true;
  }

  @Override
  protected void saveSynced(CompoundTag tags) {
    super.saveSynced(tags);
    if (texture != Blocks.AIR) {
      tags.putString(TAG_TEXTURE, getTextureName());
    }
  }

  @Override
  public void load(CompoundTag tags) {
    super.load(tags);
    if (tags.contains(TAG_TEXTURE)) {
      texture = RetexturedHelper.getBlock(tags.getStringOr(TAG_TEXTURE, ""));
      RetexturedHelper.onTextureUpdated(this);
    }
  }


  /** Fluid implementation of smeltery IO. */
  public static abstract class SmelteryFluidIO extends SmelteryInputOutputBlockEntity<ResourceHandler<FluidResource>> {
    protected SmelteryFluidIO(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state, TinkerSmeltery.SMELTERY_TANK_CAPABILITY);
    }

    @Nullable
    @Override
    protected ResourceHandler<FluidResource> getDirectMasterCapability(BlockEntity master) {
      if (master instanceof ISmelteryTankHandler tankHandler) {
        return tankHandler.getFluidCapability();
      }
      return null;
    }
  }

  /** Item implementation of smeltery IO */
  public static class ChuteBlockEntity extends SmelteryInputOutputBlockEntity<ResourceHandler<ItemResource>> {
    public ChuteBlockEntity(BlockPos pos, BlockState state) {
      this(TinkerSmeltery.chute.get(), pos, state);
    }

    protected ChuteBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state, Capabilities.Item.BLOCK);
    }
  }

}
