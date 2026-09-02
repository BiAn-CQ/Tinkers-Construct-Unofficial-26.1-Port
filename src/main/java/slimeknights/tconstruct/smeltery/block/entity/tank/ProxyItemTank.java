package slimeknights.tconstruct.smeltery.block.entity.tank;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.EmptyResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.RootCommitJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.mantle.inventory.SingleItemHandler;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.network.InventorySlotSyncPacket;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.fluid.IFluidTankUpdater;

/** Fluid handler that proxies to an item stack tank */
public class ProxyItemTank<T extends MantleBlockEntity & IFluidTankUpdater> extends SingleItemHandler<T> {
  private ResourceHandler<FluidResource> itemTank;
  private final RootCommitJournal syncJournal = new RootCommitJournal(() -> setStack(getStack(), true));
  private final ResourceHandler<FluidResource> fluidHandler = new ResourceHandler<>() {
    @Override
    public int size() {
      return getItemFluidHandler().size();
    }

    @Override
    public FluidResource getResource(int index) {
      return getItemFluidHandler().getResource(index);
    }

    @Override
    public long getAmountAsLong(int index) {
      return getItemFluidHandler().getAmountAsLong(index);
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
      return getItemFluidHandler().getCapacityAsLong(index, resource);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
      return getItemFluidHandler().isValid(index, resource);
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
      int inserted = getItemFluidHandler().insert(index, resource, amount, transaction);
      if (inserted > 0) {
        syncJournal.updateSnapshots(transaction);
      }
      return inserted;
    }

    @Override
    public int insert(FluidResource resource, int amount, TransactionContext transaction) {
      int inserted = getItemFluidHandler().insert(resource, amount, transaction);
      if (inserted > 0) {
        syncJournal.updateSnapshots(transaction);
      }
      return inserted;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
      int extracted = getItemFluidHandler().extract(index, resource, amount, transaction);
      if (extracted > 0) {
        syncJournal.updateSnapshots(transaction);
      }
      return extracted;
    }

    @Override
    public int extract(FluidResource resource, int amount, TransactionContext transaction) {
      int extracted = getItemFluidHandler().extract(resource, amount, transaction);
      if (extracted > 0) {
        syncJournal.updateSnapshots(transaction);
      }
      return extracted;
    }
  };
  private boolean forceSync;
  public ProxyItemTank(T parent) {
    super(parent, 1);
  }

  @SuppressWarnings("deprecation")
  @Override
  protected boolean isItemValid(ItemStack stack) {
    // can only store items that are fluid handlers, though allow blacklist in case something is really broken
    // blacklist is mostly used for items that don't support incremental filling, as this block really isn't good at working with them
    // we check the container item so we don't have to put every bucket in the tag. Not bothering with complex container items; odds are item stack sensitive just returns the same item
    var remainder = stack.getCraftingRemainder();
    ItemStack craftRemainingStack = remainder == null ? ItemStack.EMPTY : remainder.create();
    Item craftRemainingItem = craftRemainingStack.isEmpty() ? null : craftRemainingStack.getItem();
    return !stack.is(TinkerTags.Items.PROXY_TANK_BLACKLIST)
      && (craftRemainingItem == null || !RegistryHelper.contains(TinkerTags.Items.PROXY_TANK_BLACKLIST, craftRemainingItem))
      && slimeknights.tconstruct.library.utils.TinkerCapabilities.fluidHandler(stack) != null;
  }

  /** Used by the fluid handler logic to sync changes as we directly mutate the internal stack */
  private void setStack(ItemStack newStack, boolean syncSame) {
    // if swapping to an empty stack, switch to the empty stack instance
    // prevents accidently having a 0 stack size capability
    if (newStack.isEmpty()) {
      newStack = ItemStack.EMPTY;
    }
    forceSync = syncSame;
    try {
      super.setStack(newStack);
    } finally {
      forceSync = false;
    }
  }

  @Override
  protected void onStackChanged(ItemStack previousStack, ItemStack newStack) {
    itemTank = null;
    Level world = parent.getLevel();
    if (world != null && !world.isClientSide()
        && (forceSync || !ItemStack.matches(previousStack, newStack))) {
      parent.onTankContentsChanged();
      BlockPos pos = parent.getBlockPos();
      TinkerNetwork.getInstance().sendToClientsAround(new InventorySlotSyncPacket(newStack, 0, pos), world, pos);
    }
  }

  @Override
  public void setStack(ItemStack newStack) {
    setStack(newStack, false);
  }

  /** Gets the fluid handler for the item */
  private ResourceHandler<FluidResource> getItemFluidHandler() {
    if (itemTank == null) {
      ItemStack stack = getStack();
      itemTank = slimeknights.tconstruct.library.utils.TinkerCapabilities.fluidHandler(stack);
      if (itemTank == null) {
        itemTank = EmptyResourceHandler.instance();
      }
    }
    return itemTank;
  }

  /** Returns the native fluid view over the contained item. */
  public ResourceHandler<FluidResource> getFluidHandler() {
    return fluidHandler;
  }

}
