package slimeknights.tconstruct.smeltery.block.entity.module;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.IndexModifier;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer.IOreRate;
import slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Inventory composite made of a set of melting module inventories
 */
public class MeltingModuleInventory extends ItemStacksResourceHandler implements IndexModifier<ItemResource> {
  /**
   * There are {@link Short#MAX_VALUE} data slots in a standard container instance.
   * The smeltery requires 6 slots for fuel syncing. It also requires 3 slots per inventory slot.
   * Thus, the largest number of slots we can have without breaking syncing is the below equation.
   * This does leave us with 1 extra unused slot. If we add anything that uses more slots we will want to adjust this number.
   */
  private static final int MAX_SIZE = (Short.MAX_VALUE - 7) / 3;
  private static final String TAG_SLOT = "slot";
  private static final String TAG_ITEMS = "items";
  private static final String TAG_SIZE = "size";

  /** Parent tile entity */
  private final MantleBlockEntity parent;
  /** Fluid handler for outputs */
  protected final ResourceHandler<FluidResource> fluidHandler;
  /** Array of modules containing each slot */
  private MeltingModule[] modules;
  /** If true, module cannot be resized */
  private final boolean strictSize;
  /** Number of nuggets to produce when melting an ore */
  private final IOreRate oreRate;

  /**
   * Creates a new inventory with a fixed size
   * @param parent         Parent tile
   * @param fluidHandler   Tank for output
   * @param oreRate        Ore rate
   * @param size           Size
   */
  public MeltingModuleInventory(MantleBlockEntity parent, ResourceHandler<FluidResource> fluidHandler, IOreRate oreRate, int size) {
    super(size);
    this.parent = parent;
    this.fluidHandler = fluidHandler;
    this.modules = new MeltingModule[size];
    this.oreRate = oreRate;
    this.strictSize = size != 0;
  }

  /**
   * Creates a new inventory with a variable size
   * @param parent         Parent tile
   * @param fluidHandler   Tank for output
   * @param oreRate        Ore rate
   */
  public MeltingModuleInventory(MantleBlockEntity parent, ResourceHandler<FluidResource> fluidHandler, IOreRate oreRate) {
    this(parent, fluidHandler, oreRate, 0);
  }

  /* Properties */

  /**
   * Checks if the given slot index is valid
   * @param slot  Slot index to check
   * @return  True if valid
   */
  public boolean validSlot(int slot) {
    return slot >= 0 && slot < size();
  }

  /** Returns true if a slot is defined in the array */
  private boolean hasModule(int slot) {
    return validSlot(slot) && modules[slot] != null;
  }

  /**
   * Gets the current time of a slot
   * @param slot  Slot index
   * @return  Slot temperature
   */
  public int getCurrentTime(int slot) {
    return hasModule(slot) ? modules[slot].getCurrentTime() : 0;
  }

  /**
   * Gets the required time for a slot
   * @param slot  Slot index
   * @return  Required time
   */
  public int getRequiredTime(int slot) {
    return hasModule(slot) ? modules[slot].getRequiredTime() : 0;
  }

  /**
   * Gets the required temperature for a slot
   * @param slot  Slot index
   * @return  Required temperature
   */
  public int getRequiredTemp(int slot) {
    return hasModule(slot) ? modules[slot].getRequiredTemp() : 0;
  }


  /* Sub modules */

  /**
   * Gets the module for the given index
   * @param slot  Index
   * @return  Module for index
   * @throws IndexOutOfBoundsException  index is invalid
   */
  public MeltingModule getModule(int slot) {
    if (!validSlot(slot)) {
      throw new IndexOutOfBoundsException();
    }
    if (modules[slot] == null) {
      modules[slot] = new MeltingModule(parent, recipe -> tryFillTank(slot, recipe), oreRate, slot,
                                        stack -> set(slot, ItemResource.of(stack), stack.getCount()));
      if (!stacks.get(slot).isEmpty()) {
        modules[slot].updateStackFromInventory(stacks.get(slot));
      }
    }
    return modules[slot];
  }

  /**
   * Resizes the module to a new size
   * @param newSize        New size
   * @param stackConsumer  Consumer for any stacks that no longer fit
   * @throws IllegalStateException  If this inventory cannot be resized
   */
  public void resize(int newSize, Consumer<ItemStack> stackConsumer) {
    if (strictSize) {
      throw new IllegalStateException("Cannot resize this melting module inventory");
    }
    if (newSize > MAX_SIZE) {
      newSize = MAX_SIZE;
    }
    // nothing to do
    if (newSize == modules.length) {
      return;
    }
    // if shrinking, drop extra items
    if (newSize < modules.length) {
      for (int i = newSize; i < modules.length; i++) {
        if (modules[i] != null && !modules[i].getStack().isEmpty()) {
          stackConsumer.accept(modules[i].getStack());
        }
      }
    }

    NonNullList<ItemStack> resizedStacks = NonNullList.withSize(newSize, ItemStack.EMPTY);
    for (int i = 0; i < Math.min(newSize, stacks.size()); i++) {
      resizedStacks.set(i, stacks.get(i));
    }
    modules = Arrays.copyOf(modules, newSize);
    setStacks(resizedStacks);
    parent.setChangedFast();
  }


  @Override
  protected int getCapacity(int index, ItemResource resource) {
    return 1;
  }

  @Override
  protected void onContentsChanged(int index, ItemStack previousContents) {
    ItemStack stack = stacks.get(index);
    if (modules[index] != null || !stack.isEmpty()) {
      getModule(index).updateStackFromInventory(stack);
    } else {
      parent.setChangedFast();
    }
  }


  /* Heating */

  /**
   * Checks if any slot can heat
   * @param temperature  Temperature to try
   * @return  True if a slot can heat
   */
  public boolean canHeat(int temperature) {
    for (MeltingModule module : modules) {
      if (module != null && module.canHeatItem(temperature)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Tries to fill the fluid handler with the given fluid
   * @param index   Index of the module being filled
   * @param recipe  Recipe to add
   * @return  True if filled, false if not enough space for the whole fluid
   */
  protected boolean tryFillTank(int index, IMeltingRecipe recipe) {
    FluidStack fluid = recipe.getOutput(getModule(index));
    if (!fluid.isEmpty()) {
      try (Transaction transaction = Transaction.openRoot()) {
        int inserted = fluidHandler.insert(FluidResource.of(fluid), fluid.getAmount(), transaction);
        if (inserted == fluid.getAmount()) {
          transaction.commit();
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Heats all items in the inventory
   * @param temperature  Heating structure temperature
   */
  public void heatItems(int temperature, int rate) {
    for (MeltingModule module : modules) {
      if (module != null) {
        module.heatItem(temperature, rate);
      }
    }
  }

  /**
   * Cools down all items in the inventory, used when there is no fuel
   */
  public void coolItems() {
    for (MeltingModule module : modules) {
      if (module != null) {
        module.coolItem();
      }
    }
  }

  /**
   * Writes this module to Tag
   * @return  Module in Tag
   */
  public CompoundTag writeToTag(HolderLookup.Provider provider) {
    CompoundTag nbt = new CompoundTag();
    ListTag list = new ListTag();
    for (int i = 0; i < modules.length; i++) {
      if (modules[i] != null && !modules[i].getStack().isEmpty()) {
        CompoundTag moduleTag = modules[i].writeToTag(provider);
        moduleTag.putByte(TAG_SLOT, (byte)i);
        list.add(moduleTag);
      }
    }
    if (!list.isEmpty()) {
      nbt.put(TAG_ITEMS, list);
    }
    nbt.putByte(TAG_SIZE, (byte)modules.length);
    return nbt;
  }

  public CompoundTag writeToTag() {
    return parent.getLevel() == null ? new CompoundTag() : writeToTag(parent.getLevel().registryAccess());
  }

  /**
   * Reads this inventory from Tag
   * @param nbt  Tag compound
   */
  public void readFromTag(CompoundTag nbt, HolderLookup.Provider provider) {
    if (!strictSize) {
      int newSize = nbt.getByteOr(TAG_SIZE, (byte)0) & 255;
      if (newSize != modules.length) {
        modules = Arrays.copyOf(modules, newSize);
        setStacks(NonNullList.withSize(newSize, ItemStack.EMPTY));
      }
    }
    // remove old data
    for (int i = 0; i < modules.length; i++) {
      stacks.set(i, ItemStack.EMPTY);
      if (modules[i] != null) {
        modules[i].updateStackFromInventory(ItemStack.EMPTY);
      }
    }

    ListTag list = nbt.getListOrEmpty(TAG_ITEMS);
    for (int i = 0; i < list.size(); i++) {
      CompoundTag item = list.getCompoundOrEmpty(i);
      if (item.contains(TAG_SLOT)) {
        int slot = item.getByteOr(TAG_SLOT, (byte)0) & 255;
        if (validSlot(slot)) {
          MeltingModule module = getModule(slot);
          module.readFromTag(item, provider);
          stacks.set(slot, module.getStack());
        }
      }
    }
  }

  public void readFromTag(CompoundTag nbt) {
    if (parent.getLevel() != null) {
      readFromTag(nbt, parent.getLevel().registryAccess());
    }
  }


  /* Container sync */

  /**
   * Sets up all sub slots for tracking
   * @param consumer  IIntArray consumer
   */
  public void trackInts(Consumer<ContainerData> consumer) {
    for (int i = 0; i < size(); i++) {
      consumer.accept(getModule(i));
    }
  }
}
