package slimeknights.tconstruct.library.tools.capability.inventory;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import lombok.RequiredArgsConstructor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.EmptyResourceHandler;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStackResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.common.config.Config.ToolSyncType;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.tools.capability.ToolCapabilityProvider.IToolCapabilityProvider;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.display.ToolNameHook;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.utils.TinkerCapabilities;
import slimeknights.tconstruct.tools.menu.ToolContainerMenu;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/** Capability for a tool with an inventory */
@RequiredArgsConstructor
public class ToolInventoryCapability extends InventoryModifierHookIterator<ModifierEntry> implements ResourceHandler<ItemResource>, IndexModifier<ItemResource> {
  /** Boolean key to set in volatile mod data for the total slot count across all modifiers */
  public static final Identifier TOTAL_SLOTS = TConstruct.getResource("total_item_slots");
  /** Boolean key to set in volatile mod data to show the offand in the inventory menu */
  public static final Identifier INCLUDE_OFFHAND = TConstruct.getResource("inventory_show_offhand");
  /** Boolean key to set to enable the 3x3 crafting table in the tool inventory */
  public static final Identifier CRAFTING_TABLE = TConstruct.getResource("crafting_table");
  /** Boolean key to set to enable the 2x2 crafting table in the tool inventory */
  public static final Identifier INVENTORY_CRAFTING = TConstruct.getResource("inventory_crafting");

  /** Modifier hook instance to make an inventory modifier */
  public static final ModuleHook<InventoryModifierHook> HOOK = ModifierHooks.register(TConstruct.getResource("inventory"), InventoryModifierHook.class, InventoryModifierHookMerger::new, new InventoryModifierHook() {
    @Override
    public int getSlots(IToolStackView tool, ModifierEntry modifier) {
      return 0;
    }

    @Override
    public ItemStack getStack(IToolStackView tool, ModifierEntry modifier, int slot) {
      return ItemStack.EMPTY;
    }

    @Override
    public void setStack(IToolStackView tool, ModifierEntry modifier, int slot, ItemStack stack) {}

    @Override
    public int getSlotLimit(IToolStackView tool, ModifierEntry modifier, int slot) {
      return 0;
    }

    @Override
    public boolean isItemValid(IToolStackView tool, ModifierEntry modifier, int slot, ItemStack stack) {
      return false;
    }

    @Override
    public StackMatch findStack(IToolStackView tool, ModifierEntry modifier, Predicate<ItemStack> predicate) {
      return StackMatch.EMPTY;
    }

    @Override
    public List<ItemStack> getAllStacks(IToolStackView tool, ModifierEntry modifier, List<ItemStack> stackList) {
      return List.of();
    }
  });

  /** Supplier to the tool instance */
  private final Supplier<? extends IToolStackView> tool;
  /** Cache of all stacks that have been parsed thus far */
  private ItemStack[] cachedStacks;
  /** Transactional wrappers for each tool inventory slot. */
  private SlotHandler[] slotHandlers;

  /** Cached slot count */
  private int slots = -1;

  @Override
  public int size() {
    if (slots == -1) {
      slots = tool.get().getVolatileData().getIntOr(TOTAL_SLOTS, 0);
    }
    return slots;
  }


  /* Basic inventory */

  @Override
  protected Iterator<ModifierEntry> getIterator(IToolStackView tool) {
    // iterate in reverse order, as that allows us to put shield strap/tool belt later in the UI without breaking the keybind
    return new ReversedListIterator<>(tool.getModifierList());
  }

  @Override
  protected InventoryModifierHook getHook(ModifierEntry entry) {
    indexEntry = entry;
    return entry.getHook(HOOK);
  }

  /** If true, the given stack is blacklisted from being stored in a tool */
  public static boolean isBlacklisted(ItemStack stack) {
    return !stack.getItem().canFitInsideContainerItems() || stack.is(TinkerTags.Items.TOOL_INVENTORY_BLACKLIST) || TinkerCapabilities.itemHandler(stack) != null;
  }

  private boolean isStackValid(int slot, ItemStack stack) {
    // no nesting item handlers
    if (!stack.isEmpty() && isBlacklisted(stack)) {
      return false;
    }
    IToolStackView tool = this.tool.get();
    InventoryModifierHook inventory = findHook(tool, slot);
    if (inventory != null) {
      return inventory.isItemValid(tool, indexEntry, slot - startIndex, stack);
    }
    return false;
  }

  private int getStackCapacity(int slot) {
    IToolStackView tool = this.tool.get();
    InventoryModifierHook inventory = findHook(tool, slot);
    if (inventory != null) {
      return inventory.getSlotLimit(tool, indexEntry, slot - startIndex);
    }
    return 0;
  }


  /* Item stack cache */

  /** Clears all cached data in the capability */
  private void clearCache() {
    slots = -1;
    cachedStacks = null;
    slotHandlers = null;
  }

  /** Caches the stack in the given slot */
  private void cacheStack(int slot, ItemStack stack) {
    if (slot >= 0) {
      int slots = size();
      if (slot < slots) {
        if (cachedStacks == null) {
          cachedStacks = new ItemStack[size()];
        }
        cachedStacks[slot] = stack; // TODO: copy?
      }
    }
  }

  /** Gets the stack cached in the given slot */
  @Nullable
  private ItemStack getCachedStack(int slot) {
    if (cachedStacks != null && slot >= 0 && slot < size()) {
      return cachedStacks[slot];
    }
    return null;
  }

  /** Gets a stack from the given inventory, caching it */
  private void setAndCache(InventoryModifierHook inventory, int localSlot, int globalSlot, ItemStack stack) {
    inventory.setStack(tool.get(), indexEntry, localSlot, stack);
    // cache the stack to save lookup times later
    cacheStack(globalSlot, stack);
  }


  /* Get and set */

  private void setStack(int slot, ItemStack stack) {
    InventoryModifierHook inventory = findHook(tool.get(), slot);
    if (inventory != null) {
      setAndCache(inventory, slot - startIndex, slot, stack);
    }
  }

  /** Gets a stack from the given inventory, caching it */
  private ItemStack getAndCache(InventoryModifierHook inventory, int localSlot, int globalSlot) {
    ItemStack stack = inventory.getStack(tool.get(), indexEntry, localSlot);
    cacheStack(globalSlot, stack);
    return stack;
  }

  /** Gets the stack from cache, if failing parses it */
  private ItemStack getCached(InventoryModifierHook inventory, int localSlot, int globalSlot) {
    ItemStack stack = getCachedStack(globalSlot);
    if (stack == null) {
      stack = getAndCache(inventory, localSlot, globalSlot);
    }
    return stack;
  }

  public ItemStack getStack(int slot) {
    ItemStack cached = getCachedStack(slot);
    if (cached != null) {
      return cached;
    }
    InventoryModifierHook inventory = findHook(tool.get(), slot);
    if (inventory != null) {
      return getAndCache(inventory, slot - startIndex, slot);
    }
    return ItemStack.EMPTY;
  }

  private SlotHandler getSlotHandler(int slot) {
    Objects.checkIndex(slot, size());
    if (slotHandlers == null) {
      slotHandlers = new SlotHandler[size()];
    }
    SlotHandler handler = slotHandlers[slot];
    if (handler == null) {
      handler = new SlotHandler(slot);
      slotHandlers[slot] = handler;
    }
    return handler;
  }

  @Override
  public void set(int index, ItemResource resource, int amount) {
    if (amount < 0 || resource.isEmpty() && amount > 0) {
      throw new IllegalArgumentException("Invalid item resource amount: " + amount);
    }
    setStack(index, resource.toStack(amount));
  }

  @Override
  public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
    return getSlotHandler(index).insert(0, resource, amount, transaction);
  }

  @Override
  public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
    return getSlotHandler(index).extract(0, resource, amount, transaction);
  }

  @Override
  public ItemResource getResource(int index) {
    return getSlotHandler(index).getResource(0);
  }

  @Override
  public long getAmountAsLong(int index) {
    return getSlotHandler(index).getAmountAsLong(0);
  }

  @Override
  public long getCapacityAsLong(int index, ItemResource resource) {
    return getSlotHandler(index).getCapacityAsLong(0, resource);
  }

  @Override
  public boolean isValid(int index, ItemResource resource) {
    return getSlotHandler(index).isValid(0, resource);
  }

  private final class SlotHandler extends ItemStackResourceHandler {
    private final int index;

    private SlotHandler(int index) {
      this.index = index;
    }

    @Override
    protected ItemStack getStack() {
      return ToolInventoryCapability.this.getStack(index);
    }

    @Override
    protected void setStack(ItemStack stack) {
      ToolInventoryCapability.this.setStack(index, stack);
    }

    @Override
    protected boolean isValid(ItemResource resource) {
      return ToolInventoryCapability.this.isStackValid(index, resource.toStack());
    }

    @Override
    protected int getCapacity(ItemResource resource) {
      int capacity = ToolInventoryCapability.this.getStackCapacity(index);
      return resource.isEmpty() ? capacity : Math.min(resource.getMaxStackSize(), capacity);
    }
  }

  /** Interface for an inventory modifier to use */
  @SuppressWarnings("unused")
  public interface InventoryModifierHook {
    /** Gets the number of item slots used by the given tool. The number returned here must also be added into volatile data under {@link #TOTAL_SLOTS} */
    int getSlots(IToolStackView tool, ModifierEntry modifier);

    /** Sets the stack in the given slot */
    ItemStack getStack(IToolStackView tool, ModifierEntry modifier, int slot);

    /** Sets the stack in the given slot */
    void setStack(IToolStackView tool, ModifierEntry modifier, int slot, ItemStack stack);

    /** Gets the max stack size for the given slot */
    default int getSlotLimit(IToolStackView tool, ModifierEntry modifier, int slot) {
      return 64;
    }

    /** Checks if the item is valid for the given slot */
    default boolean isItemValid(IToolStackView tool, ModifierEntry modifier, int slot, ItemStack stack) {
      return true;
    }

    /** Gets the pattern to render when the given slot is empty */
    @Nullable
    default Pattern getPattern(IToolStackView tool, ModifierEntry modifier, int slot, boolean hasStack) {
      return null;
    }

    /**
     * Finds a matching stack in the inventory
     * @param predicate  Predicate to test
     * @return Match containing slot and stack, or {@link StackMatch#EMPTY} if not found.
     */
    default StackMatch findStack(IToolStackView tool, ModifierEntry modifier, Predicate<ItemStack> predicate) {
      for (int i = 0; i < getSlots(tool, modifier); i++) {
        ItemStack stack = getStack(tool, modifier, i);
        if (!stack.isEmpty() && predicate.test(stack)) {
          return new StackMatch(stack, i);
        }
      }
      return StackMatch.EMPTY;
    }

    /** Parses all stacks in NBT into the passed list */
    @CanIgnoreReturnValue
    default List<ItemStack> getAllStacks(IToolStackView tool, ModifierEntry modifier, List<ItemStack> stackList) {
      for (int i = 0; i < getSlots(tool, modifier); i++) {
        ItemStack stack = getStack(tool, modifier, i);
        if (!stack.isEmpty()) {
          stackList.add(stack);
        }
      }
      return stackList;
    }
  }

  /** Result of searching the inventory */
  public record StackMatch(ItemStack stack, int slot) {
    public static final StackMatch EMPTY = new StackMatch(ItemStack.EMPTY, -1);

    /** Checks if this match is empty */
    public boolean isEmpty() {
      return slot == -1;
    }
  }

  /** Merger for inventory modifier hooks */
  @RequiredArgsConstructor
  private static class InventoryModifierHookMerger extends InventoryModifierHookIterator<InventoryModifierHook> implements InventoryModifierHook {
    private final Collection<InventoryModifierHook> modules;

    @Override
    protected Iterator<InventoryModifierHook> getIterator(IToolStackView tool) {
      return modules.iterator();
    }

    @Override
    protected InventoryModifierHook getHook(InventoryModifierHook hook) {
      return hook;
    }

    /** Gets the inventory instance for the given slot index */
    @Nullable
    private InventoryModifierHook findHook(IToolStackView tool, ModifierEntry modifier, int slot) {
      indexEntry = modifier;
      return this.findHook(tool, slot);
    }

    @Override
    public int getSlots(IToolStackView tool, ModifierEntry modifier) {
      int sum = 0;
      for (InventoryModifierHook module : modules) {
        sum += module.getSlots(tool, modifier);
      }
      return sum;
    }

    @Override
    public ItemStack getStack(IToolStackView tool, ModifierEntry modifier, int slot) {
      InventoryModifierHook module = findHook(tool, modifier, slot);
      if (module != null) {
        return module.getStack(tool, modifier, slot - startIndex);
      }
      return ItemStack.EMPTY;
    }

    @Override
    public void setStack(IToolStackView tool, ModifierEntry modifier, int slot, ItemStack stack) {
      InventoryModifierHook module = findHook(tool, modifier, slot);
      if (module != null) {
        module.setStack(tool, modifier, slot - startIndex, stack);
      }
    }

    @Override
    public int getSlotLimit(IToolStackView tool, ModifierEntry modifier, int slot) {
      InventoryModifierHook module = findHook(tool, modifier, slot);
      if (module != null) {
        return module.getSlotLimit(tool, modifier, slot - startIndex);
      }
      return 0;
    }

    @Override
    public boolean isItemValid(IToolStackView tool, ModifierEntry modifier, int slot, ItemStack stack) {
      InventoryModifierHook module = findHook(tool, modifier, slot);
      if (module != null) {
        return module.isItemValid(tool, modifier, slot - startIndex, stack);
      }
      return false;
    }

    @Nullable
    @Override
    public Pattern getPattern(IToolStackView tool, ModifierEntry modifier, int slot, boolean hasStack) {
      InventoryModifierHook module = findHook(tool, modifier, slot);
      if (module != null) {
        return module.getPattern(tool, modifier, slot - startIndex, hasStack);
      }
      return null;
    }

    @Override
    public StackMatch findStack(IToolStackView tool, ModifierEntry modifier, Predicate<ItemStack> predicate) {
      int start = 0;
      for (InventoryModifierHook module : modules) {
        StackMatch match = module.findStack(tool, modifier, predicate);
        if (!match.isEmpty()) {
          return start == 0 ? match : new StackMatch(match.stack, match.slot + start);
        }
        start += module.getSlots(tool, modifier);
      }
      return StackMatch.EMPTY;
    }

    @Override
    public List<ItemStack> getAllStacks(IToolStackView tool, ModifierEntry modifier, List<ItemStack> stackList) {
      for (InventoryModifierHook module : modules) {
        module.getAllStacks(tool, modifier, stackList);
      }
      return stackList;
    }
  }

  /** Provider for an inventory tool capability */
  public static class Provider implements IToolCapabilityProvider {
    private final ToolInventoryCapability handler;
    @SuppressWarnings("unused")
    public Provider(ItemStack stack, Supplier<? extends IToolStackView> tool) {
      handler = new ToolInventoryCapability(tool);
    }

    @Override
    public <T,C> T getCapability(IToolStackView tool, ItemCapability<T,C> cap, C context) {
      if (cap == Capabilities.Item.ITEM && tool.getVolatileData().getIntOr(TOTAL_SLOTS, 0) > 0) {
        return cap.typeClass().cast(handler);
      }
      return null;
    }

    @Override
    public void clearCache() {
      handler.clearCache();
    }
  }


  /* Helpers */

  /** Adds the given number of slots to the data */
  public static void addSlots(ModDataNBT volatileData, int count) {
    volatileData.putInt(TOTAL_SLOTS, volatileData.getIntOr(TOTAL_SLOTS, 0) + count);
  }


  /** Opens the tool inventory container if an inventory is present on the given tool */
  public static InteractionResult tryOpenContainer(ItemStack stack, IToolStackView tool, Player player, EquipmentSlot slotType) {
    return tryOpenContainer(stack, tool, tool.getDefinition(), player, slotType);
  }

  /** Opens the tool inventory container if an inventory is present on the given tool */
  public static InteractionResult tryOpenContainer(ItemStack stack, @Nullable IToolStackView tool, ToolDefinition definition, Player player, EquipmentSlot slotType) {
    return tryOpenContainer(stack, tool, definition, player, switch (slotType) {
      // mainhand is the hotbar selected slot
      case MAINHAND -> player.getInventory().getSelectedSlot();
      // offhand is its own slot
      case OFFHAND -> Inventory.SLOT_OFFHAND;
      // armor starts from the end of inventory
      default -> Inventory.INVENTORY_SIZE + slotType.getIndex();
    });
  }

  /** Opens the tool inventory container if an inventory is present on the given tool */
  public static InteractionResult tryOpenContainer(ItemStack stack, @Nullable IToolStackView tool, ToolDefinition definition, Player player, int slotIndex) {
    ResourceHandler<ItemResource> capability = TinkerCapabilities.itemHandler(stack);
    ResourceHandler<ItemResource> handler = capability == null ? EmptyResourceHandler.instance() : capability;
    // open if we have any slots or we have a crafting table
    if (handler.size() > 0 || ModifierUtil.checkVolatileFlag(stack, CRAFTING_TABLE) || ModifierUtil.checkVolatileFlag(stack, INVENTORY_CRAFTING)) {
      if (player instanceof ServerPlayer serverPlayer) {
        serverPlayer.openMenu(new SimpleMenuProvider(
          (id, inventory, p) -> new ToolContainerMenu(id, inventory, stack, handler, slotIndex),
          ToolNameHook.getName(definition, stack, tool)
        ), buf -> {
          buf.writeVarInt(slotIndex);
          ToolSyncType syncType = Config.COMMON.toolInventorySync.get();
          buf.writeEnum(syncType);
          if (syncType == ToolSyncType.FULL_STACK) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
          } else if (syncType == ToolSyncType.MINIMAL) {
            buf.writeVarInt(ModifierUtil.getVolatileInt(stack, TOTAL_SLOTS));
            buf.writeEnum(CraftingType.fromStack(stack));
            buf.writeBoolean(ModifierUtil.checkVolatileFlag(stack, INCLUDE_OFFHAND));
          }
        });
      }
      return player.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }
    return InteractionResult.PASS;
  }

  /** Drops every stored stack when a tool item entity is destroyed. */
  public static void onDestroyed(ItemEntity entity) {
    if (!entity.level().isClientSide()) {
      ResourceHandler<ItemResource> handler = TinkerCapabilities.itemHandler(entity.getItem());
      if (handler != null && handler.size() > 0) {
        ItemUtils.onContainerDestroyed(entity, IntStream.range(0, handler.size())
          .mapToObj(index -> ItemUtil.getStack(handler, index))
          .filter(stack -> !stack.isEmpty()));
      }
    }
  }

  /** Iterator that goes through a list in reverse order */
  private static class ReversedListIterator<T> implements Iterator<T> {
    /** List to iterate */
    private final List<T> list;
    /** Index of the next element */
    private int index;

    public ReversedListIterator(List<T> list) {
      this.list = list;
      this.index = list.size() - 1;
    }

    @Override
    public boolean hasNext() {
      return index >= 0;
    }

    @Override
    public T next() {
      T element = list.get(index);
      index--;
      return element;
    }
  }


  /** Crafting table type for the UI */
  public enum CraftingType {
    FULL, INVENTORY, NONE;

    /** Gets the crafting type for the given stack */
    public static CraftingType fromStack(ItemStack stack) {
      if (ModifierUtil.checkVolatileFlag(stack, CRAFTING_TABLE)) {
        return FULL;
      }
      if (ModifierUtil.checkVolatileFlag(stack, INVENTORY_CRAFTING)) {
        return INVENTORY;
      }
      return NONE;
    }
  }
}
