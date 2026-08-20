package slimeknights.tconstruct.library.tools.capability.inventory;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.item.ItemPredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.IntRange;
import slimeknights.tconstruct.library.json.LevelingInt;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierRemovalHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ValidateModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.VolatileDataModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.SlotStackModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModuleBuilder;
import slimeknights.tconstruct.library.modifiers.util.ModuleWithKey;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability.InventoryModifierHook;
import slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability.StackMatch;
import slimeknights.tconstruct.library.tools.nbt.IModDataView;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolDataNBT;
import slimeknights.tconstruct.library.utils.RegistryAccessUtil;
import slimeknights.tconstruct.library.utils.ItemStackDataUtil;

import javax.annotation.Nullable;
import java.util.BitSet;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryModule implements ModifierModule, InventoryModifierHook, VolatileDataModifierHook, ValidateModifierHook, ModifierRemovalHook, ModuleWithKey, ConditionalModule<IToolContext>, SlotStackModifierHook {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<InventoryModule>defaultHooks(ToolInventoryCapability.HOOK, ModifierHooks.VOLATILE_DATA, ModifierHooks.VALIDATE, ModifierHooks.REMOVE, ModifierHooks.SLOT_STACK);
  /** Mod Data NBT mapper to get a compound list */
  public static final BiFunction<CompoundTag,String,ListTag> GET_COMPOUND_LIST = CompoundTag::getListOrEmpty;
  /** Error for if the container has items preventing modifier removal */
  private static final Component HAS_ITEMS = TConstruct.makeTranslation("modifier", "inventory_cannot_remove");
  /** NBT key to store the slot for a stack */
  public static final String TAG_SLOT = "Slot";
  // fields
  protected static final LoadableField<Identifier,? super InventoryModule> KEY_FIELD = ModuleWithKey.FIELD;
  protected static final LoadableField<LevelingInt, InventoryModule> SLOTS_FIELD = LevelingInt.LOADABLE.requiredField("slots", InventoryModule::slots);
  protected static final LoadableField<LevelingInt, InventoryModule> LIMIT_FIELD = LevelingInt.LOADABLE.defaultField("limit", LevelingInt.flat(64), InventoryModule::slotLimit);
  protected static final LoadableField<IJsonPredicate<Item>, InventoryModule> FILTER_FIELD = ItemPredicate.LOADER.defaultField("filter", InventoryModule::filter);
  protected static final LoadableField<Pattern, InventoryModule> PATTERN_FIELD = Pattern.PARSER.nullableField("pattern", InventoryModule::pattern);
  protected static final LoadableField<IntRange,InventoryModule> VALIDATION_FIELD = ModifierEntry.VALID_LEVEL.defaultField("validation_level", InventoryModule::validationLevel);
  /** Loader instance */
  public static final RecordLoadable<InventoryModule> LOADER = RecordLoadable.create(KEY_FIELD, SLOTS_FIELD, LIMIT_FIELD, FILTER_FIELD, PATTERN_FIELD, ModifierCondition.CONTEXT_FIELD, VALIDATION_FIELD, InventoryModule::new);

  /** Module adding an inventory to a tool */
  private final @Nullable Identifier key;
  /** Location to save the inventory */
  private final LevelingInt slots;
  /** Slots to add to the tool */
  private final LevelingInt slotLimit;
  /** Maximum stack size in each slot */
  private final IJsonPredicate<Item> filter;
  /** Filter for valid items in the slot */
  private final @Nullable Pattern pattern;
  /** Slot background to show */
  private final ModifierCondition<IToolContext> condition;
  /** Additional conditions */
  private final IntRange validationLevel;

  public Identifier key() { return key; }
  public LevelingInt slots() { return slots; }
  public LevelingInt slotLimit() { return slotLimit; }
  public IJsonPredicate<Item> filter() { return filter; }
  public Pattern pattern() { return pattern; }
  public ModifierCondition<IToolContext> condition() { return condition; }
  public IntRange validationLevel() { return validationLevel; }

  @Override
  public RecordLoadable<? extends InventoryModule> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }


  /* Properties */

  /** Gets the number of slots at the given level, assuming this module is active */
  private int getPotentialSlots(float level) {
    return Math.max(0, slots.computeForLevel(level));
  }

  @Override
  public int getSlots(IToolStackView tool, ModifierEntry modifier) {
    return condition.matches(tool, modifier) ? getPotentialSlots(modifier.getEffectiveLevel()) : 0;
  }

  @Override
  public void addVolatileData(IToolContext context, ModifierEntry modifier, ToolDataNBT volatileData) {
    if (condition.matches(context, modifier)) {
      ToolInventoryCapability.addSlots(volatileData, getPotentialSlots(modifier.getEffectiveLevel()));
    }
  }

  @Override
  public int getSlotLimit(IToolStackView tool, ModifierEntry modifier, int slot) {
    return slotLimit.compute(modifier.getEffectiveLevel());
  }

  @Override
  public boolean isItemValid(IToolStackView tool, ModifierEntry modifier, int slot, ItemStack stack) {
    return condition.matches(tool, modifier) && filter.matches(stack.getItem());
  }

  @Nullable
  @Override
  public Pattern getPattern(IToolStackView tool, ModifierEntry modifier, int slot, boolean hasStack) {
    return hasStack ? null : pattern;
  }


  /* Inventory */

  @Override
  public ItemStack getStack(IToolStackView tool, ModifierEntry modifier, int slot) {
    IModDataView modData = tool.getPersistentData();
    Identifier key = getKey(modifier.getModifier());
    if (slot < getSlots(tool, modifier) && modData.contains(key)) {
      ListTag list = tool.getPersistentData().get(key, GET_COMPOUND_LIST);
      for (int i = 0; i < list.size(); i++) {
        CompoundTag compound = list.getCompoundOrEmpty(i);
        if (compound.getIntOr(TAG_SLOT, 0) == slot) {
          return compound.contains("id")
            ? ItemStackDataUtil.parse(RegistryAccessUtil.getRegistryAccess(), compound)
            : ItemStack.EMPTY;
        }
      }
    }
    return ItemStack.EMPTY;
  }

  @Override
  public void setStack(IToolStackView tool, ModifierEntry modifier, int slot, ItemStack stack) {
    if (slot < 0 || slot >= getSlots(tool, modifier)) {
      return;
    }

    ModDataNBT modData = tool.getPersistentData();
    Identifier key = getKey(modifier.getModifier());
    ListTag list = modData.contains(key) ? modData.get(key, GET_COMPOUND_LIST) : new ListTag();
    int insertIndex = 0;

    // first, try to find an existing stack in the slot
    for (int i = 0; i < list.size(); i++) {
      CompoundTag compound = list.getCompoundOrEmpty(i);
      int listSlot = compound.getIntOr(TAG_SLOT, 0);
      if (listSlot == slot) {
        if (stack.isEmpty()) {
          list.remove(i);
          if (list.isEmpty()) {
            modData.remove(key);
          } else {
            // ListTag is mutable, so explicitly put it back to notify component-backed ToolStack data.
            modData.put(key, list);
          }
        } else {
          list.set(i, writeStack(stack, slot, new CompoundTag()));
          // ListTag is mutable, so explicitly put it back to notify component-backed ToolStack data.
          modData.put(key, list);
        }
        return;
      }
      // try to keep the stacks in order by inserting after the last slot smaller than the target
      if (listSlot < slot) {
        insertIndex = i + 1;
      }
    }

    // list did not contain the slot, so add it unless the new stack is empty
    if (stack.isEmpty()) {
      return;
    }
    CompoundTag compound = writeStack(stack, slot, new CompoundTag());
    // if out of bounds, just put at the end. Shouldn't happen
    if (insertIndex > list.size()) {
      list.add(compound);
    } else {
      list.add(insertIndex, compound);
    }
    // Do this after mutating the list. Putting an empty list first would sync before the item was added.
    modData.put(key, list);
  }


  /* Validation */

  @Nullable
  @Override
  public Component validate(IToolStackView tool, ModifierEntry modifier) {
    // don't validate if the module is not running
    if (condition.tool().matches(tool) && validationLevel.test(modifier.getLevel())) {
      IModDataView persistentData = tool.getPersistentData();
      Identifier key = getKey(modifier.getModifier());
      int maxSlots = getSlots(tool, modifier);
      if (persistentData.contains(key)) {
        ListTag listNBT = persistentData.get(key, GET_COMPOUND_LIST);
        if (!listNBT.isEmpty()) {
          if (maxSlots == 0) {
            return HAS_ITEMS;
          }
          // first, see whether we have any available slots
          BitSet freeSlots = new BitSet(maxSlots);
          freeSlots.set(0, maxSlots, true);
          for (int i = 0; i < listNBT.size(); i++) {
            freeSlots.set(listNBT.getCompoundOrEmpty(i).getIntOr(TAG_SLOT, 0), false);
          }
          for (int i = 0; i < listNBT.size(); i++) {
            CompoundTag compoundNBT = listNBT.getCompoundOrEmpty(i);
            if (compoundNBT.getIntOr(TAG_SLOT, 0) >= maxSlots) {
              int free = freeSlots.stream().findFirst().orElse(-1);
              if (free == -1) {
                return HAS_ITEMS;
              } else {
                freeSlots.set(free, false);
                compoundNBT.putInt(TAG_SLOT, free);
              }
            }
          }
        }
      }
    }
    return null;
  }

  @Nullable
  @Override
  public Component onRemoved(IToolStackView tool, Modifier modifier) {
    // if we currently have item data, then return an error
    ModDataNBT persistentData = tool.getPersistentData();
    Identifier key = getKey(modifier);
    if (persistentData.contains(key) && !persistentData.get(key, GET_COMPOUND_LIST).isEmpty()) {
      return HAS_ITEMS;
    }
    // remove the data key, should be empty
    persistentData.remove(key);
    return null;
  }


  /* Helpers */

  /**
   * Writes a stack to NBT, including the slot
   * @param stack  Stack to write
   * @param slot   Target slot
   * @param compound  Tag to write to, use new for new tag
   * @return Tag written to, same as {@code compound}.
   */
  public static CompoundTag writeStack(ItemStack stack, int slot, CompoundTag compound) {
    CompoundTag encodedCompound = ItemStackDataUtil.save(RegistryAccessUtil.getRegistryAccess(), stack, compound);
    encodedCompound.putInt(TAG_SLOT, slot);
    return encodedCompound;
  }

  @Override
  public StackMatch findStack(IToolStackView tool, ModifierEntry modifier, Predicate<ItemStack> predicate) {
    // can save a lot of effort if we have no slots
    int max = getSlots(tool, modifier);
    if (max > 0) {
      IModDataView persistentData = tool.getPersistentData();
      Identifier key = getKey(modifier.getModifier());
      ListTag slots = persistentData.get(key, GET_COMPOUND_LIST);
      if (!slots.isEmpty()) {
        // search all slots for the first match
        for (int i = 0; i < slots.size(); i++) {
          CompoundTag compound = slots.getCompoundOrEmpty(i);
          // slot must be valid
          int slot = compound.getIntOr(TAG_SLOT, 0);
          if (slot < max) {
            ItemStack stack = compound.contains("id")
              ? ItemStackDataUtil.parse(RegistryAccessUtil.getRegistryAccess(), compound)
              : ItemStack.EMPTY;
            if (!stack.isEmpty() && predicate.test(stack)) {
              return new StackMatch(stack, slot);
            }
          }
        }
      }
    }
    return StackMatch.EMPTY;
  }

  @Override
  public List<ItemStack> getAllStacks(IToolStackView tool, ModifierEntry entry, List<ItemStack> stackList) {
    // can save a lot of effort if we have no slots
    int max = getSlots(tool, entry);
    if (max > 0) {
      IModDataView modData = tool.getPersistentData();
      Identifier key = getKey(entry.getModifier());
      if (modData.contains(key)) {
        ListTag list = modData.get(key, GET_COMPOUND_LIST);

        // make sure the stacks are in order, NBT could store them in any order
        ItemStack[] parsed = new ItemStack[max];
        for (int i = 0; i < list.size(); i++) {
          CompoundTag compound = list.getCompoundOrEmpty(i);
          // slot must be valid
          int slot = compound.getIntOr(TAG_SLOT, 0);
          if (slot < max) {
            parsed[slot] = compound.contains("id")
              ? ItemStackDataUtil.parse(RegistryAccessUtil.getRegistryAccess(), compound)
              : ItemStack.EMPTY;
          }
        }
        // add stacks into the list
        for (ItemStack stack : parsed) {
          if (stack != null && !stack.isEmpty()) {
            stackList.add(stack);
          }
        }
      }
    }
    return stackList;
  }


  /* Slot interaction */

  @Override
  public boolean overrideOtherStackedOnMe(IToolStackView slotTool, ModifierEntry modifier, ItemStack held, Slot slot, Player player, SlotAccess access) {
    if (getSlots(slotTool, modifier) > 0) {
      return InventorySlotMenuModule.INSTANCE.overrideOtherStackedOnMe(slotTool, modifier, held, slot, player, access);
    }
    return false;
  }

  /* Builder */

  /** Creates a new builder instance */
  public static Builder builder() {
    return new Builder();
  }

  @Accessors(fluent = true)
  @Setter
  public static class Builder extends ModuleBuilder.Context<Builder> {
    @Nullable
    protected Identifier key = null;
    protected LevelingInt slotLimit = LevelingInt.flat(64);
    protected IJsonPredicate<Item> filter = ItemPredicate.ANY;
    @Nullable
    protected Pattern pattern = null;
    protected IntRange validationLevel = ModifierEntry.VALID_LEVEL;

    public Builder key(Identifier key) { this.key = key; return this; }
    public Builder slotLimit(LevelingInt slotLimit) { this.slotLimit = slotLimit; return this; }
    public Builder filter(IJsonPredicate<Item> filter) { this.filter = filter; return this; }
    public Builder pattern(Pattern pattern) { this.pattern = pattern; return this; }
    public Builder validationLevel(IntRange validationLevel) { this.validationLevel = validationLevel; return this; }

    protected Builder() {}

    /** Copies properties from the given module, excluding slots which is terminal. */
    public Builder from(InventoryModule inventory) {
      this.key = inventory.key;
      this.slotLimit = inventory.slotLimit;
      this.filter = inventory.filter;
      this.pattern = inventory.pattern;
      this.validationLevel = inventory.validationLevel;
      this.condition = inventory.condition;
      return this;
    }

    /** Sets the base slot limit */
    public Builder flatLimit(int limit) {
      return slotLimit(LevelingInt.flat(limit));
    }

    /** Sets the base slot limit */
    public Builder limitPerLevel(int limit) {
      return slotLimit(LevelingInt.eachLevel(limit));
    }

    /** Builds the final instance */
    public InventoryModule slots(LevelingInt slots) {
      return new InventoryModule(key, slots, slotLimit, filter, pattern, condition, validationLevel);
    }

    /** Builds the final instance */
    public InventoryModule slots(int base, int perLevel) {
      return slots(new LevelingInt(base, perLevel));
    }

    /** Builds the final instance */
    public InventoryModule flatSlots(int slots) {
      return slots(slots, 0);
    }

    /** Builds the final instance */
    public InventoryModule slotsPerLevel(int slots) {
      return slots(0, slots);
    }
  }
}
