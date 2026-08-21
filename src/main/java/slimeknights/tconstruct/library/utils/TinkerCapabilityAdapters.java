package slimeknights.tconstruct.library.utils;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import slimeknights.tconstruct.library.fluid.IndexedFluidHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Bridges the legacy Forge handlers still used by Tinkers to the 26.1 transfer API.
 *
 * <p>The native 26.1 capability is a {@link ResourceHandler}; the legacy wrappers
 * are retained at the edges of the port so the gameplay code can be migrated in
 * smaller, verifiable steps.</p>
 */
public final class TinkerCapabilityAdapters {
  private TinkerCapabilityAdapters() {}

  public static IItemHandler itemHandler(ResourceHandler<ItemResource> handler) {
    if (handler == null) {
      return null;
    }
    // Keep extra interfaces and state exposed by legacy handlers, such as the
    // scaling chest's visual slot count, when converting our own wrapper back.
    if (handler instanceof LegacyItemResourceHandler legacy) {
      return legacy.handler;
    }
    return new ModifiableItemHandlerAdapter(handler);
  }

  public static IFluidHandler fluidHandler(ResourceHandler<FluidResource> handler) {
    return handler == null ? null : IFluidHandler.of(handler);
  }

  public static ResourceHandler<ItemResource> itemResource(IItemHandler handler) {
    return handler == null ? null : new LegacyItemResourceHandler(handler);
  }

  public static ResourceHandler<FluidResource> fluidResource(IFluidHandler handler) {
    return handler == null ? null : new LegacyFluidResourceHandler(handler);
  }

  /**
   * Bridges a legacy item fluid handler to the contextual 26.1 item capability.
   *
   * <p>An item capability is created from a snapshot of the current stack. Mutating
   * that snapshot does not update the player's hand or inventory slot. Recreate the
   * legacy handler from the current {@link ItemAccess} contents for every operation,
   * then exchange the updated item resource in the caller's transaction.</p>
   */
  public static ResourceHandler<FluidResource> fluidItemResource(
    ItemAccess itemAccess, Function<ItemStack,? extends IFluidHandlerItem> handlerFactory
  ) {
    if (itemAccess == null || itemAccess.getResource().isEmpty()) {
      return null;
    }
    IFluidHandlerItem handler = handlerFactory.apply(itemAccess.getResource().toStack());
    return handler == null ? null : new LegacyFluidItemResourceHandler(itemAccess, handlerFactory, handler.getTanks());
  }

  /**
   * Bridges the native transfer API to the legacy handler API used by menu slots.
   *
   * <p>NeoForge's stock {@code IItemHandler.of} adapter intentionally exposes a
   * read-only {@code IItemHandler}.  {@code SlotItemHandler} still calls
   * {@code setStackInSlot} while a container receives its initial contents, so a
   * menu backed by that adapter disconnects on 26.1.  The underlying transfer
   * handler already provides transactional insert/extract operations; exposing
   * those operations through the modifiable bridge keeps slot synchronization
   * valid without unsafe casts.</p>
   */
  private static final class ModifiableItemHandlerAdapter implements IItemHandlerModifiable {
    private final ResourceHandler<ItemResource> handler;

    private ModifiableItemHandlerAdapter(ResourceHandler<ItemResource> handler) {
      this.handler = handler;
    }

    @Override
    public int getSlots() {
      return handler.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
      ItemResource resource = handler.getResource(slot);
      return resource.isEmpty() ? ItemStack.EMPTY : resource.toStack(handler.getAmountAsInt(slot));
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
      if (stack.isEmpty() || !handler.isValid(slot, ItemResource.of(stack))) {
        return stack;
      }
      int amount = stack.getCount();
      try (Transaction transaction = Transaction.openRoot()) {
        int inserted = handler.insert(slot, ItemResource.of(stack), amount, transaction);
        if (!simulate) {
          transaction.commit();
        }
        return inserted >= amount ? ItemStack.EMPTY : stack.copyWithCount(amount - inserted);
      }
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
      if (amount <= 0) {
        return ItemStack.EMPTY;
      }
      ItemResource resource = handler.getResource(slot);
      if (resource.isEmpty()) {
        return ItemStack.EMPTY;
      }
      try (Transaction transaction = Transaction.openRoot()) {
        int extracted = handler.extract(slot, resource, amount, transaction);
        if (!simulate) {
          transaction.commit();
        }
        return resource.toStack(extracted);
      }
    }

    @Override
    public int getSlotLimit(int slot) {
      return handler.getCapacityAsInt(slot, handler.getResource(slot));
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
      return stack.isEmpty() || handler.isValid(slot, ItemResource.of(stack));
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
      ItemResource replacement = ItemResource.of(stack);
      if (!stack.isEmpty() && !handler.isValid(slot, replacement)) {
        throw new IllegalArgumentException("Item " + stack + " is not valid for slot " + slot);
      }
      ItemResource current = handler.getResource(slot);
      int currentAmount = handler.getAmountAsInt(slot);
      try (Transaction transaction = Transaction.openRoot()) {
        if (!current.isEmpty() && currentAmount > 0) {
          int extracted = handler.extract(slot, current, currentAmount, transaction);
          if (extracted != currentAmount) {
            throw new IllegalStateException("Could not fully clear slot " + slot + ": expected "
              + currentAmount + " items, extracted " + extracted);
          }
        }
        if (!stack.isEmpty()) {
          int inserted = handler.insert(slot, replacement, stack.getCount(), transaction);
          if (inserted != stack.getCount()) {
            throw new IllegalStateException("Could not fully replace slot " + slot + ": expected "
              + stack.getCount() + " items, inserted " + inserted);
          }
        }
        transaction.commit();
      }
    }
  }

  /** Transactional view over a legacy, slot-addressable item handler. */
  private static final class LegacyItemResourceHandler extends SnapshotJournal<ItemTransactionSnapshot> implements ResourceHandler<ItemResource> {
    private final IItemHandler handler;
    private boolean transactionOpen;
    private List<ItemStack> initialStacks = List.of();
    private List<ItemStack> transactionStacks = List.of();
    private boolean[] touchedSlots = new boolean[0];
    private List<ItemOperation> operations = new ArrayList<>();

    private LegacyItemResourceHandler(IItemHandler handler) {
      this.handler = handler;
    }

    @Override
    public int size() {
      return handler.getSlots();
    }

    @Override
    public ItemResource getResource(int index) {
      return ItemResource.of(getStack(index));
    }

    @Override
    public long getAmountAsLong(int index) {
      return getStack(index).getCount();
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
      return handler.getSlotLimit(index);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
      return resource.isEmpty() || index >= 0 && index < size() && handler.isItemValid(index, resource.toStack(1));
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
      if (resource.isEmpty() || amount <= 0) {
        return 0;
      }
      beginTransaction(transaction);
      if (index < 0 || index >= transactionStacks.size() || !isValid(index, resource)) {
        return 0;
      }

      ItemStack current = transactionStacks.get(index);
      if (!current.isEmpty() && !resource.matches(current)) {
        return 0;
      }
      int capacity = Math.min(handler.getSlotLimit(index), resource.toStack(1).getMaxStackSize());
      int accepted = Math.min(amount, Math.max(0, capacity - current.getCount()));
      if (!touchedSlots[index]) {
        ItemStack remainder = handler.insertItem(index, resource.toStack(amount), true);
        accepted = Math.min(accepted, amount - remainder.getCount());
      }
      if (accepted <= 0) {
        return 0;
      }

      touchedSlots[index] = true;
      operations.add(new ItemOperation(index, true, resource, accepted));
      if (current.isEmpty()) {
        transactionStacks.set(index, resource.toStack(accepted));
      } else {
        current.grow(accepted);
      }
      return accepted;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
      if (resource.isEmpty() || amount <= 0) {
        return 0;
      }
      beginTransaction(transaction);
      if (index < 0 || index >= transactionStacks.size()) {
        return 0;
      }
      ItemStack current = transactionStacks.get(index);
      if (current.isEmpty() || !resource.matches(current)) {
        return 0;
      }

      int extracted = Math.min(amount, current.getCount());
      if (!touchedSlots[index]) {
        ItemStack simulated = handler.extractItem(index, amount, true);
        if (!simulated.isEmpty() && !resource.matches(simulated)) {
          return 0;
        }
        extracted = Math.min(extracted, simulated.getCount());
      }
      if (extracted <= 0) {
        return 0;
      }

      touchedSlots[index] = true;
      operations.add(new ItemOperation(index, false, resource, extracted));
      current.shrink(extracted);
      if (current.isEmpty()) {
        transactionStacks.set(index, ItemStack.EMPTY);
      }
      return extracted;
    }

    private ItemStack getStack(int index) {
      if (transactionOpen) {
        return index >= 0 && index < transactionStacks.size() ? transactionStacks.get(index) : ItemStack.EMPTY;
      }
      return index >= 0 && index < handler.getSlots() ? handler.getStackInSlot(index) : ItemStack.EMPTY;
    }

    private void beginTransaction(TransactionContext transaction) {
      updateSnapshots(transaction);
      if (!transactionOpen) {
        initialStacks = snapshotItems(handler);
        transactionStacks = copyItems(initialStacks);
        touchedSlots = new boolean[initialStacks.size()];
        operations = new ArrayList<>();
        transactionOpen = true;
      }
    }

    @Override
    protected ItemTransactionSnapshot createSnapshot() {
      return new ItemTransactionSnapshot(transactionOpen, copyItems(initialStacks), copyItems(transactionStacks),
        touchedSlots.clone(), copyItemOperations(operations));
    }

    @Override
    protected void revertToSnapshot(ItemTransactionSnapshot snapshot) {
      transactionOpen = snapshot.transactionOpen();
      initialStacks = copyItems(snapshot.initialStacks());
      transactionStacks = copyItems(snapshot.stacks());
      touchedSlots = snapshot.touchedSlots().clone();
      operations = copyItemOperations(snapshot.operations());
    }

    @Override
    protected void onRootCommit(ItemTransactionSnapshot originalState) {
      List<ItemStack> before = copyItems(initialStacks);
      List<ItemStack> expected = copyItems(transactionStacks);
      List<ItemOperation> committed = copyItemOperations(operations);
      transactionOpen = false;
      initialStacks = List.of();
      transactionStacks = List.of();
      touchedSlots = new boolean[0];
      operations = new ArrayList<>();

      verifyItemState(before, "changed before transaction commit");
      for (ItemOperation operation : committed) {
        if (operation.insert()) {
          ItemStack remainder = handler.insertItem(operation.index(), operation.resource().toStack(operation.amount()), false);
          int inserted = operation.amount() - remainder.getCount();
          if (inserted != operation.amount()) {
            throw new IllegalStateException("Legacy item handler accepted " + operation.amount()
              + " items during simulation but inserted " + inserted + " on commit in slot " + operation.index()
              + ": " + handler.getClass().getName());
          }
        } else {
          ItemStack extracted = handler.extractItem(operation.index(), operation.amount(), false);
          if (extracted.getCount() != operation.amount() || !operation.resource().matches(extracted)) {
            throw new IllegalStateException("Legacy item handler exposed " + operation.amount()
              + " items during transaction but extracted " + extracted.getCount() + " on commit from slot "
              + operation.index() + ": " + handler.getClass().getName());
          }
        }
      }
      verifyItemState(expected, "did not reach the committed state");
    }

    private void verifyItemState(List<ItemStack> expected, String problem) {
      if (handler.getSlots() != expected.size()) {
        throw new IllegalStateException("Legacy item handler " + problem + ": slot count changed from "
          + expected.size() + " to " + handler.getSlots() + " in " + handler.getClass().getName());
      }
      for (int slot = 0; slot < expected.size(); slot++) {
        if (!sameItemAndCount(expected.get(slot), handler.getStackInSlot(slot))) {
          throw new IllegalStateException("Legacy item handler " + problem + " in slot " + slot + ": "
            + handler.getClass().getName());
        }
      }
    }
  }

  /** Transactional item-backed view over a legacy fluid handler. */
  private static final class LegacyFluidItemResourceHandler extends ItemAccessResourceHandler<FluidResource> {
    private final Item validItem;
    private final Function<ItemStack,? extends IFluidHandlerItem> handlerFactory;

    private LegacyFluidItemResourceHandler(
      ItemAccess itemAccess, Function<ItemStack,? extends IFluidHandlerItem> handlerFactory, int tanks
    ) {
      super(itemAccess, tanks);
      this.validItem = itemAccess.getResource().getItem();
      this.handlerFactory = handlerFactory;
    }

    private IFluidHandlerItem getHandler(ItemResource accessResource) {
      return accessResource.is(validItem) ? handlerFactory.apply(accessResource.toStack()) : null;
    }

    @Override
    protected FluidResource getResourceFrom(ItemResource accessResource, int index) {
      IFluidHandlerItem handler = getHandler(accessResource);
      return handler == null || index >= handler.getTanks()
        ? FluidResource.EMPTY
        : FluidResource.of(handler.getFluidInTank(index));
    }

    @Override
    protected int getAmountFrom(ItemResource accessResource, int index) {
      IFluidHandlerItem handler = getHandler(accessResource);
      return handler == null || index >= handler.getTanks() ? 0 : handler.getFluidInTank(index).getAmount();
    }

    @Override
    protected ItemResource update(
      ItemResource accessResource, int index, FluidResource newResource, int newAmount
    ) {
      IFluidHandlerItem handler = getHandler(accessResource);
      if (handler == null || index >= handler.getTanks()) {
        return ItemResource.EMPTY;
      }

      List<FluidStack> before = snapshotFluids(handler);
      FluidStack current = before.get(index);
      int currentAmount = current.getAmount();
      if (newAmount > currentAmount) {
        if (!current.isEmpty() && !newResource.matches(current)) {
          return ItemResource.EMPTY;
        }
        int requested = newAmount - currentAmount;
        if (handler.fill(newResource.toStack(requested), IFluidHandler.FluidAction.EXECUTE) != requested) {
          return ItemResource.EMPTY;
        }
      } else if (newAmount < currentAmount) {
        int requested = currentAmount - newAmount;
        FluidStack drained = handler.drain(current.copyWithAmount(requested), IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() != requested || !FluidStack.isSameFluidSameComponents(drained, current)) {
          return ItemResource.EMPTY;
        }
      }

      List<FluidStack> after = snapshotFluids(handler);
      if (!matchesUpdatedTank(after.get(index), newResource, newAmount)) {
        return ItemResource.EMPTY;
      }
      for (int tank = 0; tank < before.size(); tank++) {
        if (tank != index && !sameFluidAndAmount(before.get(tank), after.get(tank))) {
          return ItemResource.EMPTY;
        }
      }
      return ItemResource.of(handler.getContainer());
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
      IFluidHandlerItem handler = getHandler(itemAccess.getResource());
      return handler != null && index < handler.getTanks()
        && handler.isFluidValid(index, resource.toStack(1));
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
      IFluidHandlerItem handler = getHandler(itemAccess.getResource());
      return handler == null || index >= handler.getTanks() ? 0 : handler.getTankCapacity(index);
    }

    private static List<FluidStack> snapshotFluids(IFluidHandler handler) {
      List<FluidStack> fluids = new ArrayList<>(handler.getTanks());
      for (int tank = 0; tank < handler.getTanks(); tank++) {
        fluids.add(handler.getFluidInTank(tank).copy());
      }
      return fluids;
    }

    private static boolean matchesUpdatedTank(FluidStack stack, FluidResource resource, int amount) {
      return amount == 0 ? stack.isEmpty() : stack.getAmount() == amount && resource.matches(stack);
    }

    private static boolean sameFluidAndAmount(FluidStack first, FluidStack second) {
      return first.getAmount() == second.getAmount()
        && (first.isEmpty() ? second.isEmpty() : FluidStack.isSameFluidSameComponents(first, second));
    }
  }

  /**
   * Transactional view over a legacy fluid handler.
   *
   * <p>Legacy handlers expose simulation as an explicit action, while the new
   * transfer API represents simulation by aborting a transaction. Mutating the
   * legacy handler immediately would make every aborted probe consume or insert
   * real fluid. Keep a logical view during the transaction and replay only
   * committed operations against the legacy handler.</p>
   */
  private static final class LegacyFluidResourceHandler extends SnapshotJournal<FluidTransactionSnapshot> implements ResourceHandler<FluidResource> {
    private final IFluidHandler handler;
    private boolean transactionOpen;
    private List<FluidStack> initialFluids = List.of();
    private List<FluidStack> transactionFluids = List.of();
    private int[] transactionCapacities = new int[0];
    private List<FluidOperation> operations = new ArrayList<>();

    private LegacyFluidResourceHandler(IFluidHandler handler) {
      this.handler = handler;
    }

    @Override
    public int size() {
      return transactionOpen ? transactionFluids.size() : handler.getTanks();
    }

    @Override
    public FluidResource getResource(int index) {
      return FluidResource.of(getFluid(index));
    }

    @Override
    public long getAmountAsLong(int index) {
      return getFluid(index).getAmount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
      if (transactionOpen) {
        return index >= 0 && index < transactionCapacities.length ? transactionCapacities[index] : 0;
      }
      return index >= 0 && index < handler.getTanks() ? handler.getTankCapacity(index) : 0;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
      return resource.isEmpty() || (index >= 0 && index < size() && handler.isFluidValid(index, resource.toStack(1)));
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
      if (resource.isEmpty() || amount <= 0) {
        return 0;
      }
      beginTransaction(transaction);
      if (index < 0 || index >= transactionFluids.size() || !isValid(index, resource)) {
        return 0;
      }

      FluidStack current = transactionFluids.get(index);
      if (!current.isEmpty() && !resource.matches(current)) {
        return 0;
      }

      int capacity = transactionCapacities[index];
      int currentAmount = current.getAmount();
      int accepted;
      if (capacity <= currentAmount) {
        // Recipe-sized legacy tanks may report zero capacity while empty. Ask
        // the original handler to validate the resource and determine the size.
        if (!current.isEmpty()) {
          return 0;
        }
        accepted = fill(index, resource.toStack(amount), IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
          return 0;
        }
        capacity = accepted;
        transactionCapacities[index] = capacity;
      } else {
        accepted = Math.min(amount, capacity - currentAmount);
        // On the first operation, retain any additional rules implemented by
        // the legacy handler beyond capacity and isFluidValid().
        if (!hasOperation(index)) {
          accepted = Math.min(accepted, fill(index, resource.toStack(amount), IFluidHandler.FluidAction.SIMULATE));
        }
      }
      if (accepted <= 0) {
        return 0;
      }

      FluidStack inserted = resource.toStack(accepted);
      operations.add(new FluidOperation(index, true, inserted));
      if (current.isEmpty()) {
        transactionFluids.set(index, inserted.copy());
      } else {
        current.grow(accepted);
      }
      return accepted;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
      if (resource.isEmpty() || amount <= 0) {
        return 0;
      }
      beginTransaction(transaction);
      if (index < 0 || index >= transactionFluids.size()) {
        return 0;
      }

      FluidStack current = transactionFluids.get(index);
      if (current.isEmpty() || !resource.matches(current)) {
        return 0;
      }
      int extracted = Math.min(amount, current.getAmount());
      if (!hasOperation(index)) {
        FluidStack simulated = drain(index, current.copyWithAmount(extracted), IFluidHandler.FluidAction.SIMULATE);
        if (simulated.isEmpty() || !FluidStack.isSameFluidSameComponents(simulated, current)) {
          return 0;
        }
        extracted = Math.min(extracted, simulated.getAmount());
      }
      if (extracted <= 0) {
        return 0;
      }

      FluidStack extractedStack = current.copyWithAmount(extracted);
      operations.add(new FluidOperation(index, false, extractedStack));
      current.shrink(extracted);
      if (current.isEmpty()) {
        transactionFluids.set(index, FluidStack.EMPTY);
      }
      return extracted;
    }

    private FluidStack getFluid(int index) {
      if (transactionOpen) {
        return index >= 0 && index < transactionFluids.size() ? transactionFluids.get(index) : FluidStack.EMPTY;
      }
      return index >= 0 && index < handler.getTanks() ? handler.getFluidInTank(index) : FluidStack.EMPTY;
    }

    private void beginTransaction(TransactionContext transaction) {
      if (!transactionOpen) {
        int tanks = handler.getTanks();
        transactionFluids = new ArrayList<>(tanks);
        transactionCapacities = new int[tanks];
        for (int i = 0; i < tanks; i++) {
          transactionFluids.add(handler.getFluidInTank(i).copy());
          transactionCapacities[i] = handler.getTankCapacity(i);
        }
        initialFluids = copyFluids(transactionFluids);
        operations = new ArrayList<>();
        // Capture the closed state so aborting the root transaction closes the
        // logical view, while retaining the initial fluid snapshot for commit
        // validation.
        this.updateSnapshots(transaction);
        transactionOpen = true;
      } else {
        this.updateSnapshots(transaction);
      }
    }

    @Override
    protected FluidTransactionSnapshot createSnapshot() {
      return new FluidTransactionSnapshot(transactionOpen, copyFluids(initialFluids), copyFluids(transactionFluids),
        transactionCapacities.clone(), copyOperations(operations));
    }

    @Override
    protected void revertToSnapshot(FluidTransactionSnapshot snapshot) {
      transactionOpen = snapshot.transactionOpen();
      initialFluids = copyFluids(snapshot.initialFluids());
      transactionFluids = copyFluids(snapshot.fluids());
      transactionCapacities = snapshot.capacities().clone();
      operations = copyOperations(snapshot.operations());
    }

    @Override
    protected void onRootCommit(FluidTransactionSnapshot originalState) {
      List<FluidOperation> committed = copyOperations(operations);
      List<FluidStack> expectedInitial = copyFluids(initialFluids);
      transactionOpen = false;
      initialFluids = List.of();
      transactionFluids = List.of();
      transactionCapacities = new int[0];
      operations = new ArrayList<>();

      List<FluidStack> actualInitial = snapshotFluids(handler);
      if (!sameFluids(expectedInitial, actualInitial)) {
        throw new IllegalStateException("Legacy fluid handler changed while a transfer transaction was open: "
          + handler.getClass().getName());
      }

      for (FluidOperation operation : committed) {
        FluidStack stack = operation.stack();
        if (operation.insert()) {
          int inserted = fill(operation.index(), stack, IFluidHandler.FluidAction.EXECUTE);
          if (inserted != stack.getAmount()) {
            throw new IllegalStateException("Legacy fluid handler accepted " + stack.getAmount()
              + " mB during simulation but inserted " + inserted + " mB on commit: " + handler.getClass().getName());
          }
        } else {
          FluidStack extracted = drain(operation.index(), stack, IFluidHandler.FluidAction.EXECUTE);
          if (extracted.getAmount() != stack.getAmount() || !FluidStack.isSameFluidSameComponents(extracted, stack)) {
            throw new IllegalStateException("Legacy fluid handler exposed " + stack.getAmount()
              + " mB during transaction but extracted " + extracted.getAmount() + " mB on commit: " + handler.getClass().getName());
          }
        }
      }
    }

    private boolean hasOperation(int index) {
      for (FluidOperation operation : operations) {
        if (operation.index() == index) {
          return true;
        }
      }
      return false;
    }

    private int fill(int index, FluidStack stack, IFluidHandler.FluidAction action) {
      if (handler instanceof IndexedFluidHandler indexed) {
        return indexed.fill(index, stack, action);
      }
      return index == 0 && handler.getTanks() == 1 ? handler.fill(stack, action) : 0;
    }

    private FluidStack drain(int index, FluidStack stack, IFluidHandler.FluidAction action) {
      if (handler instanceof IndexedFluidHandler indexed) {
        return indexed.drain(index, stack, action);
      }
      return index == 0 && handler.getTanks() == 1 ? handler.drain(stack, action) : FluidStack.EMPTY;
    }

    private static List<FluidStack> snapshotFluids(IFluidHandler handler) {
      List<FluidStack> fluids = new ArrayList<>(handler.getTanks());
      for (int tank = 0; tank < handler.getTanks(); tank++) {
        fluids.add(handler.getFluidInTank(tank).copy());
      }
      return fluids;
    }

    private static boolean sameFluids(List<FluidStack> first, List<FluidStack> second) {
      if (first.size() != second.size()) {
        return false;
      }
      for (int i = 0; i < first.size(); i++) {
        FluidStack left = first.get(i);
        FluidStack right = second.get(i);
        if (left.getAmount() != right.getAmount()
          || (!left.isEmpty() && !FluidStack.isSameFluidSameComponents(left, right))
          || (left.isEmpty() != right.isEmpty())) {
          return false;
        }
      }
      return true;
    }

    private static List<FluidStack> copyFluids(List<FluidStack> fluids) {
      List<FluidStack> copy = new ArrayList<>(fluids.size());
      for (FluidStack fluid : fluids) {
        copy.add(fluid.copy());
      }
      return copy;
    }

    private static List<FluidOperation> copyOperations(List<FluidOperation> source) {
      List<FluidOperation> copy = new ArrayList<>(source.size());
      for (FluidOperation operation : source) {
        copy.add(new FluidOperation(operation.index(), operation.insert(), operation.stack().copy()));
      }
      return copy;
    }
  }

  private static List<ItemStack> snapshotItems(IItemHandler handler) {
    List<ItemStack> stacks = new ArrayList<>(handler.getSlots());
    for (int slot = 0; slot < handler.getSlots(); slot++) {
      stacks.add(handler.getStackInSlot(slot).copy());
    }
    return stacks;
  }

  private static List<ItemStack> copyItems(List<ItemStack> source) {
    List<ItemStack> copy = new ArrayList<>(source.size());
    for (ItemStack stack : source) {
      copy.add(stack.copy());
    }
    return copy;
  }

  private static List<ItemOperation> copyItemOperations(List<ItemOperation> source) {
    return new ArrayList<>(source);
  }

  private static boolean sameItemAndCount(ItemStack first, ItemStack second) {
    return first.getCount() == second.getCount()
      && (first.isEmpty() ? second.isEmpty() : ItemStack.isSameItemSameComponents(first, second));
  }

  private record ItemOperation(int index, boolean insert, ItemResource resource, int amount) {}

  private record ItemTransactionSnapshot(boolean transactionOpen, List<ItemStack> initialStacks, List<ItemStack> stacks,
                                         boolean[] touchedSlots, List<ItemOperation> operations) {}

  private record FluidOperation(int index, boolean insert, FluidStack stack) {}

  private record FluidTransactionSnapshot(boolean transactionOpen, List<FluidStack> initialFluids,
                                          List<FluidStack> fluids, int[] capacities,
                                          List<FluidOperation> operations) {}
}
