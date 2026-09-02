package slimeknights.tconstruct.library.tools.capability.fluid;

import lombok.RequiredArgsConstructor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.capability.ToolCapabilityProvider.IToolCapabilityProvider;
import slimeknights.tconstruct.library.tools.nbt.IModDataView;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Logic to make a tool a fluid handler
 */
public class ToolFluidCapability extends FluidModifierHookIterator<ModifierEntry> implements ResourceHandler<FluidResource> {
  /** Boolean key to set in volatile mod data to enable the fluid capability */
  public static final Identifier TOTAL_TANKS = TConstruct.getResource("total_tanks");

  /** Modifier hook instance to make an inventory modifier */
  public static final ModuleHook<FluidModifierHook> HOOK = ModifierHooks.register(TConstruct.getResource("fluid"), FluidModifierHook.class, FluidModifierHookMerger::new, new FluidModifierHook() {
    @Override
    public int getTanks(IModDataView volatileData, ModifierEntry modifier) {
      return 0;
    }

    @Override
    public boolean isFluidValid(IToolStackView tool, ModifierEntry modifier, int tank, FluidStack fluid) {
      return false;
    }

    @Override
    public int insert(IToolStackView tool, ModifierEntry modifier, int tank, FluidStack resource) {
      return 0;
    }

    @Override
    public FluidStack extract(IToolStackView tool, ModifierEntry modifier, int tank, FluidStack resource) {
      return FluidStack.EMPTY;
    }

    @Override
    public FluidStack extract(IToolStackView tool, ModifierEntry modifier, int tank, int maxDrain) {
      return FluidStack.EMPTY;
    }
  });

  private final ItemAccess itemAccess;
  private final Item validItem;

  public ToolFluidCapability(ItemAccess itemAccess) {
    this.itemAccess = itemAccess;
    this.validItem = itemAccess.getResource().getItem();
  }

  /* Basic inventory */

  @Override
  public int size() {
    return itemAccess.getResource().is(validItem)
      ? getTool().getVolatileData().getIntOr(TOTAL_TANKS, 0)
      : 0;
  }

  private IToolStackView getTool() {
    return ToolStack.from(itemAccess.getResource().toStack());
  }

  @Override
  protected Iterator<ModifierEntry> getIterator(IToolStackView tool) {
    return tool.getModifierList().iterator();
  }

  @Override
  protected FluidModifierHook getHook(ModifierEntry entry) {
    indexEntry = entry;
    return entry.getHook(HOOK);
  }

  @Override
  public FluidResource getResource(int tank) {
    Objects.checkIndex(tank, size());
    IToolStackView tool = getTool();
    FluidModifierHook hook = findHook(tool, tank);
    if (hook != null) {
      return FluidResource.of(hook.getFluidInTank(tool, indexEntry, tank - startIndex));
    }
    return FluidResource.EMPTY;
  }

  @Override
  public long getAmountAsLong(int tank) {
    Objects.checkIndex(tank, size());
    IToolStackView tool = getTool();
    FluidModifierHook hook = findHook(tool, tank);
    if (hook != null) {
      return (long) itemAccess.getAmount()
        * hook.getFluidInTank(tool, indexEntry, tank - startIndex).getAmount();
    }
    return 0;
  }

  @Override
  public long getCapacityAsLong(int tank, FluidResource resource) {
    Objects.checkIndex(tank, size());
    IToolStackView tool = getTool();
    FluidModifierHook hook = findHook(tool, tank);
    if (hook != null && (resource.isEmpty()
        || hook.isFluidValid(tool, indexEntry, tank - startIndex, resource.toStack(1)))) {
      return (long) itemAccess.getAmount()
        * hook.getTankCapacity(tool, indexEntry, tank - startIndex);
    }
    return 0;
  }

  @Override
  public boolean isValid(int tank, FluidResource resource) {
    Objects.checkIndex(tank, size());
    TransferPreconditions.checkNonEmpty(resource);
    IToolStackView tool = getTool();
    FluidModifierHook hook = findHook(tool, tank);
    return hook != null && hook.isFluidValid(tool, indexEntry, tank - startIndex, resource.toStack(1));
  }

  @Override
  public int insert(int tank, FluidResource resource, int amount, TransactionContext transaction) {
    Objects.checkIndex(tank, size());
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    int itemCount = itemAccess.getAmount();
    if (itemCount == 0) {
      return 0;
    }
    int amountPerItem = amount / itemCount;
    if (amountPerItem <= 0) {
      return 0;
    }

    ItemStack updatedStack = itemAccess.getResource().toStack();
    IToolStackView updatedTool = ToolStack.from(updatedStack);
    FluidModifierHook hook = findHook(updatedTool, tank);
    if (hook == null) {
      return 0;
    }
    int insertedPerItem = hook.insert(updatedTool, indexEntry, tank - startIndex,
      resource.toStack(amountPerItem));
    if (insertedPerItem <= 0 || insertedPerItem > amountPerItem) {
      return 0;
    }
    int exchanged = itemAccess.exchange(ItemResource.of(updatedStack), itemCount, transaction);
    return exchanged == itemCount ? insertedPerItem * itemCount : 0;
  }

  @Override
  public int extract(int tank, FluidResource resource, int amount, TransactionContext transaction) {
    Objects.checkIndex(tank, size());
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    int itemCount = itemAccess.getAmount();
    if (itemCount == 0) {
      return 0;
    }
    int amountPerItem = amount / itemCount;
    if (amountPerItem <= 0) {
      return 0;
    }

    ItemStack updatedStack = itemAccess.getResource().toStack();
    IToolStackView updatedTool = ToolStack.from(updatedStack);
    FluidModifierHook hook = findHook(updatedTool, tank);
    if (hook == null) {
      return 0;
    }
    FluidStack extracted = hook.extract(updatedTool, indexEntry, tank - startIndex,
      resource.toStack(amountPerItem));
    if (extracted.isEmpty() || extracted.getAmount() > amountPerItem || !resource.matches(extracted)) {
      return 0;
    }
    int exchanged = itemAccess.exchange(ItemResource.of(updatedStack), itemCount, transaction);
    return exchanged == itemCount ? extracted.getAmount() * itemCount : 0;
  }

  /** Adds the tanks from the fluid modifier to the tool */
  public static void addTanks(ModifierEntry modifier, ModDataNBT volatileData, FluidModifierHook hook) {
    volatileData.putInt(TOTAL_TANKS, hook.getTanks(volatileData, modifier) + volatileData.getIntOr(TOTAL_TANKS, 0));
  }

  /**
   * Interface for modifiers with fluid capabilities to return.
     * @deprecated We are considering removing this interface in favor of a much simpler tool tank implementation.
     * For most use-cases {@link ToolTankHelper} is sufficient.
   */
  @SuppressWarnings("unused")
  @Deprecated
  public interface FluidModifierHook {
    /**
     * Determines how many fluid tanks are used by this modifier
     * @param volatileData  Tool data to check
     * @param modifier      Modifier to consider
     * @return  Number of tanks used
     */
    default int getTanks(IModDataView volatileData, ModifierEntry modifier) {
      return 1;
    }

    /**
     * Gets the fluid in the given tank
     * @param tool      Tool instance
     * @param modifier  Entry instance
     * @param tank      Tank index
     * @return  Fluid in the given tank
     */
    default FluidStack getFluidInTank(IToolStackView tool, ModifierEntry modifier, int tank) {
      return FluidStack.EMPTY;
    }

    /**
     * Gets the max capacity for the given tank
     * @param tool      Tool instance
     * @param modifier  Entry instance
     * @param tank      Tank index
     * @return  Fluid in the given tank
     */
    default int getTankCapacity(IToolStackView tool, ModifierEntry modifier, int tank) {
      return 0;
    }

    /**
     * Checks if the fluid is valid for the given tank
     * @param tool      Tool instance
     * @param modifier  Entry instance
     * @param tank      Tank index
     * @param fluid  Fluid to insert
     * @return  True if the fluid is valid
     */
    default boolean isFluidValid(IToolStackView tool, ModifierEntry modifier, int tank, FluidStack fluid) {
      return true;
    }

    /**
     * Inserts fluid into one tank.
     * @param tool      Tool instance
     * @param modifier  Entry instance
     * @param tank      Local tank index for this hook
     * @param resource  Fluid and maximum amount to insert
     * @return Amount of resource that was (or would have been, if simulated) filled.
     */
    int insert(IToolStackView tool, ModifierEntry modifier, int tank, FluidStack resource);

    /**
     * Extracts a matching fluid from one tank.
     * @param tool      Tool instance
     * @param modifier  Entry instance
     * @param tank      Local tank index for this hook
     * @param resource  Fluid and maximum amount to extract
     * @return FluidStack representing the Fluid and amount that was (or would have been, if
     * simulated) drained.
     */
    FluidStack extract(IToolStackView tool, ModifierEntry modifier, int tank, FluidStack resource);

    /**
     * Extracts any fluid from one tank.
     * @param tool      Tool instance
     * @param modifier  Entry instance
     * @param tank      Local tank index for this hook
     * @param maxDrain  Maximum amount of fluid to extract
     * @return FluidStack representing the Fluid and amount that was (or would have been, if
     * simulated) drained.
     */
    FluidStack extract(IToolStackView tool, ModifierEntry modifier, int tank, int maxDrain);
  }

  /** Logic to merge multiple fluid hooks */
  @RequiredArgsConstructor
  private static class FluidModifierHookMerger extends FluidModifierHookIterator<FluidModifierHook> implements FluidModifierHook {
    private final Collection<FluidModifierHook> modules;

    @Override
    protected Iterator<FluidModifierHook> getIterator(IToolStackView tool) {
      return modules.iterator();
    }

    @Override
    protected FluidModifierHook getHook(FluidModifierHook entry) {
      return entry;
    }

    /** Gets the given hook */
    @Nullable
    private FluidModifierHook findHook(IToolStackView tool, ModifierEntry modifier, int tank) {
      indexEntry = modifier;
      return this.findHook(tool, tank);
    }

    @Override
    public int getTanks(IModDataView volatileData, ModifierEntry modifier) {
      int sum = 0;
      for (FluidModifierHook module : modules) {
        sum += module.getTanks(volatileData, modifier);
      }
      return sum;
    }

    @Override
    public FluidStack getFluidInTank(IToolStackView tool, ModifierEntry modifier, int tank) {
      FluidModifierHook hook = findHook(tool, modifier, tank);
      if (hook != null) {
        return hook.getFluidInTank(tool, modifier, tank - startIndex);
      }
      return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(IToolStackView tool, ModifierEntry modifier, int tank) {
      FluidModifierHook hook = findHook(tool, modifier, tank);
      if (hook != null) {
        return hook.getTankCapacity(tool, modifier, tank - startIndex);
      }
      return 0;
    }

    @Override
    public boolean isFluidValid(IToolStackView tool, ModifierEntry modifier, int tank, FluidStack fluid) {
      FluidModifierHook hook = findHook(tool, modifier, tank);
      if (hook != null) {
        return hook.isFluidValid(tool, modifier, tank - startIndex, fluid);
      }
      return false;
    }

    @Override
    public int insert(IToolStackView tool, ModifierEntry modifier, int tank, FluidStack resource) {
      FluidModifierHook hook = findHook(tool, modifier, tank);
      return hook == null ? 0 : hook.insert(tool, modifier, tank - startIndex, resource);
    }

    @Override
    public FluidStack extract(IToolStackView tool, ModifierEntry modifier, int tank, FluidStack resource) {
      FluidModifierHook hook = findHook(tool, modifier, tank);
      return hook == null ? FluidStack.EMPTY : hook.extract(tool, modifier, tank - startIndex, resource);
    }

    @Override
    public FluidStack extract(IToolStackView tool, ModifierEntry modifier, int tank, int maxDrain) {
      FluidModifierHook hook = findHook(tool, modifier, tank);
      return hook == null ? FluidStack.EMPTY : hook.extract(tool, modifier, tank - startIndex, maxDrain);
    }
  }

  /** Provider instance for a fluid cap */
  public static class Provider implements IToolCapabilityProvider {
    @SuppressWarnings("unused")
    public Provider(ItemStack stack, Supplier<? extends IToolStackView> toolStack) {
    }

    @Override
    public <T,C> T getCapability(IToolStackView tool, ItemCapability<T,C> cap, C context) {
      if (cap == Capabilities.Fluid.ITEM && context instanceof ItemAccess itemAccess
          && tool.getVolatileData().getIntOr(TOTAL_TANKS, 0) > 0) {
        return cap.typeClass().cast(new ToolFluidCapability(itemAccess));
      }
      return null;
    }
  }
}
