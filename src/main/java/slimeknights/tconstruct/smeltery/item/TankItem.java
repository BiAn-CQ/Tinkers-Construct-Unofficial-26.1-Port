package slimeknights.tconstruct.smeltery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.mantle.fluid.tooltip.FluidTooltipHandler;
import slimeknights.mantle.fluid.transfer.FluidContainerTransferManager;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferDirection;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferResult;
import slimeknights.mantle.item.BlockTooltipItem;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.utils.NBTTags;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock.TankType;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TankItem extends BlockTooltipItem {
  public static final String FLUID_ID = TConstruct.makeTranslationKey("item", "tank.fluid_id");
  private static final Predicate<FluidStack> NO_FILL = FluidStack::isEmpty;
  private final boolean limitStackSize;
  public TankItem(Block blockIn, Properties builder, boolean limitStackSize) {
    super(blockIn, builder);
    this.limitStackSize = limitStackSize;
  }

  /** Checks if the tank item is filled */
  private static boolean isFilled(ItemStack stack) {
    // has a container if not empty
    SimpleFluidContent fluid = stack.get(TinkerModule.FLUID_STACK_COMPONENT.get());
    return fluid != null && !fluid.isEmpty();
  }

  public boolean hasCraftingRemainingItem(ItemStack stack) {
    return isFilled(stack);
  }

  public ItemStack getCraftingRemainingItem(ItemStack stack) {
    return isFilled(stack) ? new ItemStack(this) : ItemStack.EMPTY;
  }

  @Override
  public int getMaxStackSize(ItemStack stack) {
    if (!limitStackSize) {
      return super.getMaxStackSize(stack);
    }
    return isFilled(stack) ? 16: 64;
  }

  @Override
  public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
    List<Component> lines = new java.util.ArrayList<>();
    if (isFilled(stack)) {
      ScaledFluidTank tank = getTank(stack, 1);
      if (tank.getFluidAmount() > 0) {
        FluidStack fluid = tank.getFluid();
        lines.add(fluid.getHoverName().plainCopy().withStyle(ChatFormatting.GRAY));
        if (flag.isAdvanced()) {
          lines.add(Component.translatable(FLUID_ID, Loadables.FLUID.getKey(fluid.getFluid())).withStyle(ChatFormatting.DARK_GRAY));
        }
        FluidTooltipHandler.appendMaterial(fluid, lines);
      }
    }
    else {
      super.appendHoverText(stack, context, display, tooltip, flag);
    }
    lines.forEach(tooltip);
  }

  public ResourceHandler<FluidResource> getFluidHandler(ItemAccess itemAccess) {
    return new TankItemFluidHandler(this, itemAccess);
  }

  /** Whether a stack of this tank may be filled without exceeding its filled stack limit. */
  boolean canFill(int count) {
    return !limitStackSize || count <= 16;
  }

  /** Checks if the given stack has fluid transfer */
  public static boolean mayHaveFluid(ItemStack stack) {
    return FluidContainerTransferManager.INSTANCE.mayHaveTransfer(stack) || slimeknights.tconstruct.library.utils.TinkerCapabilities.fluidHandler(stack) != null;
  }

  @Override
  public boolean overrideStackedOnOther(ItemStack held, Slot slot, ClickAction action, Player player) {
    // take over right click, assuming the target has an item. If not, then we want to place 1 item in the slot
    if (action == ClickAction.SECONDARY && slot.allowModification(player)) {
      ItemStack slotStack = slot.getItem();
      // if it's the same item, we might want to transfer fluid or just move 1 item; overrideOtherStackedOnMe will handle deciding which to take
      if (!slotStack.isEmpty() && held.getItem() != slotStack.getItem() && mayHaveFluid(slotStack)) {
        // target must be stack size 1, if not then it's not safe to modify it
        if (slotStack.getCount() == 1) {
          // transfer fluid - but we work with just 1 tank at a time instead of trying to transfer the whole stack
          ScaledFluidTank tank = getTank(held, 1);
          TransferResult result = FluidTransferHelper.interactWithStack(tank, slotStack, TransferDirection.REVERSE);
          // update held tank and slot item if something changed
          if (result != null) {
            // play sound
            if (player.level().isClientSide()) {
              player.playSound(result.getSound());
            }
            // update stack
            slot.set(FluidTransferHelper.getOrTransferFilled(player, slotStack, result.stack()));
            // deal with remainder
            if (held.getCount() == 1) {
              setTank(held, tank);
            } else {
              // if we have multiple, toss the update anywhere
              ItemStack split = held.split(1);
              setTank(split, tank);
              if (!player.getInventory().add(split)) {
                player.drop(split, false);
              }
            }
          }
        } else {
          // we don't try filling items with a larger stack size as our transfer logic does not support that
          // however, supposing that item accepts it in their stack on me logic, let them respond
          // this won't run twice as we will be returning true regardless
          if (slotStack.isItemEnabled(player.level().enabledFeatures())) {
            AbstractContainerMenu menu = player.containerMenu;
            slotStack.overrideOtherStackedOnMe(held, slot, action, player, new SlotAccess() {
              @Override
              public ItemStack get() {
                return menu.getCarried();
              }

              @Override
              public boolean set(ItemStack stack) {
                menu.setCarried(stack);
                return true;
              }
            });
          }
        }
        return true;
      }
    }
    return false;
  }

  /** Updates the item the player is holding from the old instance */
  public static void updateHeldItem(Player player, ItemStack held, ItemStack result) {
    if (player.containerMenu.getCarried() == held) {
      player.containerMenu.setCarried(FluidTransferHelper.getOrTransferFilled(player, held, result));
    } else if (!player.getInventory().add(result)) {
      player.drop(result, false);
    }
  }

  @Override
  public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack held, Slot slot, ClickAction action, Player player, SlotAccess pAccess) {
    // take over right click, unless there is no held item (we still want split stack support)
    if (action == ClickAction.SECONDARY && slot.allowModification(player) && !held.isEmpty() && mayHaveFluid(held)) {
      // we can safely modify tanks of size larger than 1,
      // though our fluid transfer logic does not handle well transferring between two tanks with no 1mb increments
      if (stack.getCount() == 1 || held.getItem() instanceof TankItem) {
        // transfer the fluid
        ScaledFluidTank tank = getTank(stack);
        // if both tanks are empty, just do standard stack operations; makes it nice and easy to move just 1 item at a time
        if (tank.isEmpty() && ItemStack.isSameItemSameComponents(stack, held)) {
          return false;
        }
        TransferResult result = FluidTransferHelper.interactWithStack(tank, held, TransferDirection.AUTO);
        if (result != null) {
          // play sound
          if (player.level().isClientSide()) {
            player.playSound(result.getSound());
          }
          // update tank
          setTank(stack, tank);
          // update held item, assuming its actually held
          updateHeldItem(player, held, result.stack());
        }
      }
      return true;
    }
    return false;
  }

  /** Removes the tank from the given stack */
  private static void removeTank(ItemStack stack) {
    stack.remove(TinkerModule.FLUID_STACK_COMPONENT.get());
  }

  /**
   * Sets the tank to the given stack
   * @param stack  Stack
   * @param tank   Tank instance
   * @return  Stack with tank
   */
  public static ItemStack setTank(ItemStack stack, ResourceHandler<FluidResource> tank) {
    FluidStack stored = tank.size() == 0 ? FluidStack.EMPTY : FluidUtil.getStack(tank, 0);
    if (stored.isEmpty()) {
      removeTank(stack);
    } else {
      FluidStack fluid = stored.copyWithAmount(stored.getAmount() / stack.getCount());
      stack.set(TinkerModule.FLUID_STACK_COMPONENT.get(), SimpleFluidContent.copyOf(fluid));
    }
    return stack;
  }

  /**
   * Sets the tank to the given stack
   * @param stack  Stack
   * @param fluid  Fluid
   * @return  Stack with tank
   */
  public static ItemStack setTank(ItemStack stack, FluidStack fluid) {
    if (fluid.isEmpty()) {
      removeTank(stack);
    } else {
      stack.set(TinkerModule.FLUID_STACK_COMPONENT.get(), SimpleFluidContent.copyOf(fluid));
    }
    return stack;
  }

  /** Creates a stack with the given fluid and amount, not validated. */
  private static ItemStack setTank(ItemLike item, Identifier fluid, int amount) {
    ItemStack stack = new ItemStack(item);
    Fluid value = BuiltInRegistries.FLUID.getValue(fluid);
    if (value != null && value != Fluids.EMPTY) {
      stack.set(TinkerModule.FLUID_STACK_COMPONENT.get(), SimpleFluidContent.copyOf(new FluidStack(value, amount)));
    }
    return stack;
  }

  /**
   * Gets the tank for the given stack, scaled by the stack size.
   * @param stack  Tank stack
   * @return  Tank stored in the stack
   */
  public ScaledFluidTank getTank(ItemStack stack) {
    int count = stack.getCount();
    ScaledFluidTank tank = getTank(stack, count);
    // disallow filling if the current size is larger than 16
    if (limitStackSize && count > 16) {
      tank.setValidator(NO_FILL);
    }
    return tank;
  }

  /**
   * Gets the tank for the given stack
   * @param stack  Tank stack
   * @param scale  Number of tanks in a stack, being filled or drained together.
   * @return  Tank stored in the stack
   */
  public static ScaledFluidTank getTank(ItemStack stack, int scale) {
    ScaledFluidTank tank = ScaledFluidTank.create(TankBlockEntity.getCapacity(stack.getItem()), scale);
    SimpleFluidContent stored = stack.get(TinkerModule.FLUID_STACK_COMPONENT.get());
    if (stored != null && !stored.isEmpty()) {
      FluidStack fluid = stored.copy();
      fluid.setAmount(fluid.getAmount() * scale);
      tank.setFluid(fluid);
    }
    return tank;
  }

  /**
   * Gets a string variant name for the given stack
   * @param stack  Stack instance to check
   * @return  String variant name
   */
  public static String getSubtype(ItemStack stack) {
    SimpleFluidContent fluid = stack.get(TinkerModule.FLUID_STACK_COMPONENT.get());
    return fluid == null || fluid.isEmpty() ? "" : BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString();
  }

  /** Adds filled variants of all standard tank items to the given consumer */
  @SuppressWarnings("deprecation")
  public static void addFilledVariants(Consumer<ItemStack> output) {
    for (Fluid fluid : BuiltInRegistries.FLUID) {
      var holder = fluid.builtInRegistryHolder();
      if (fluid.isSource(fluid.defaultFluidState()) && !holder.is(TinkerTags.Fluids.HIDE_IN_CREATIVE_TANKS)) {
      // use an ingot variety for metals
      TankType tank, gauge;
      if (holder.is(TinkerTags.Fluids.METAL_TOOLTIPS)) {
        tank = TankType.INGOT_TANK;
        gauge = TankType.INGOT_GAUGE;
      } else {
        tank = TankType.FUEL_TANK;
        gauge = TankType.FUEL_GAUGE;
      }
      Identifier fluidName = BuiltInRegistries.FLUID.getKey(fluid);
      output.accept(setTank(TinkerSmeltery.searedLantern, fluidName, FluidValues.LANTERN_CAPACITY));
      output.accept(fillTank(TinkerSmeltery.searedTank, tank, fluidName));
      output.accept(fillTank(TinkerSmeltery.searedTank, gauge, fluidName));
      output.accept(setTank(TinkerSmeltery.scorchedLantern, fluidName, FluidValues.LANTERN_CAPACITY));
      output.accept(fillTank(TinkerSmeltery.scorchedTank, tank, fluidName));
      output.accept(fillTank(TinkerSmeltery.scorchedTank, gauge, fluidName));
      }
    }
  }

  /** Fills a tank stack with the given fluid */
  public static ItemStack fillTank(EnumObject<TankType,? extends ItemLike> tank, TankType type, Fluid fluid) {
    return setTank(new ItemStack(tank.get(type)), new FluidStack(fluid, type.getCapacity()));
  }

  /** Fills a tank stack with the given fluid */
  public static ItemStack fillTank(EnumObject<TankType,? extends ItemLike> tank, TankType type, Identifier fluid) {
    return setTank(tank.get(type), fluid, type.getCapacity());
  }
}
