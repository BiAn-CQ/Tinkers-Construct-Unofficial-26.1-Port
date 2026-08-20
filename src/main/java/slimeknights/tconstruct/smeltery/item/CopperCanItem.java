package slimeknights.tconstruct.smeltery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fluid container holding 1 ingot of fluid
 */
public class CopperCanItem extends Item {
  private static final String TAG_FLUID = "fluid";
  private static final String TAG_FLUID_TAG = "fluid_tag";

  public CopperCanItem(Properties properties) {
    super(properties);
  }

  public IFluidHandlerItem getFluidHandler(ItemStack stack) {
    return new CopperCanFluidHandler(stack);
  }

  public boolean hasCraftingRemainingItem(ItemStack stack) {
    return getFluid(stack) != Fluids.EMPTY;
  }

  public ItemStack getCraftingRemainingItem(ItemStack stack) {
    if (hasCraftingRemainingItem(stack)) {
      return new ItemStack(this);
    }
    return ItemStack.EMPTY;
  }

  @Override
  public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
    List<Component> lines = new java.util.ArrayList<>();
    Fluid fluid = getFluid(stack);
    if (fluid != Fluids.EMPTY) {
      CompoundTag fluidTag = getFluidTag(stack);
      MutableComponent text;
      if (fluidTag != null) {
        SimpleFluidContent displayFluid = stack.get(TinkerModule.FLUID_STACK_COMPONENT.get());
        text = displayFluid.copy().getHoverName().plainCopy();
      } else {
        text = Component.translatable(fluid.getFluidType().getDescriptionId());
      }
      lines.add(Component.translatable(this.getDescriptionId() + ".contents", text).withStyle(ChatFormatting.GRAY));
      if (flag.isAdvanced()) {
        lines.add(Component.translatable(TankItem.FLUID_ID, Loadables.FLUID.getKey(fluid)).withStyle(ChatFormatting.DARK_GRAY));
      }
    } else {
      lines.add(Component.translatable(this.getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
    }
    lines.forEach(tooltip);
  }

  /** Removes the fluid from the given stack */
  public static void removeFluid(ItemStack stack) {
    stack.remove(TinkerModule.FLUID_STACK_COMPONENT.get());
  }

  /** Sets the fluid on the given stack whether or not its valiid */
  private static void setFluidInternal(ItemStack stack, Identifier fluid, @Nullable CompoundTag fluidTag) {
    Fluid value = BuiltInRegistries.FLUID.getValue(fluid);
    if (value != null && value != Fluids.EMPTY) {
      DataComponentPatch patch = fluidTag == null ? DataComponentPatch.EMPTY : DataComponentPatch.builder()
        .set(DataComponents.CUSTOM_DATA, CustomData.of(fluidTag.copy())).build();
      stack.set(TinkerModule.FLUID_STACK_COMPONENT.get(), SimpleFluidContent.copyOf(new FluidStack(BuiltInRegistries.FLUID.wrapAsHolder(value), FluidValues.INGOT, patch)));
    }
  }


  /** Sets the fluid on the given stack */
  @SuppressWarnings("deprecation")
  public static ItemStack setFluid(ItemStack stack, Identifier fluid, @Nullable CompoundTag fluidTag) {
    // if empty, try to remove the NBT, helps with recipes
    if (fluid.equals(BuiltInRegistries.FLUID.getDefaultKey())) {
      removeFluid(stack);
    } else {
      setFluidInternal(stack, fluid, fluidTag);
    }
    return stack;
  }
  /** Sets the fluid on the given stack */
  @SuppressWarnings("deprecation")
  public static ItemStack setFluid(ItemStack stack, Fluid fluid, @Nullable CompoundTag fluidTag) {
    // if empty, try to remove the NBT, helps with recipes
    if (fluid == Fluids.EMPTY) {
      removeFluid(stack);
    } else {
      setFluidInternal(stack, BuiltInRegistries.FLUID.getKey(fluid), fluidTag);
    }
    return stack;
  }

  /** Sets the fluid on the given stack */
  public static ItemStack setFluid(ItemStack stack, FluidStack fluid) {
    if (fluid.isEmpty()) {
      removeFluid(stack);
    } else {
      stack.set(TinkerModule.FLUID_STACK_COMPONENT.get(), SimpleFluidContent.copyOf(fluid.copyWithAmount(FluidValues.INGOT)));
    }
    return stack;
  }

  /** Gets the fluid from the given stack */
  public static Fluid getFluid(ItemStack stack) {
    SimpleFluidContent fluid = stack.get(TinkerModule.FLUID_STACK_COMPONENT.get());
    return fluid == null || fluid.isEmpty() ? Fluids.EMPTY : fluid.getFluid();
  }

  /** Adds filled variants of the copper can to the given consumer */
  @SuppressWarnings("deprecation")
  public static void addFilledVariants(Consumer<ItemStack> output) {
    for (Fluid fluid : BuiltInRegistries.FLUID) {
      var holder = fluid.builtInRegistryHolder();
      if (fluid.isSource(fluid.defaultFluidState()) && !holder.is(TinkerTags.Fluids.HIDE_IN_CREATIVE_TANKS)) {
        output.accept(CopperCanItem.setFluid(new ItemStack(TinkerSmeltery.copperCan), BuiltInRegistries.FLUID.getKey(fluid), null));
      }
    }
  }

  /** Gets the fluid NBT from the given stack */
  @Nullable
  public static CompoundTag getFluidTag(ItemStack stack) {
    SimpleFluidContent fluid = stack.get(TinkerModule.FLUID_STACK_COMPONENT.get());
    if (fluid == null) {
      return null;
    }
    CustomData data = fluid.get(DataComponents.CUSTOM_DATA);
    return data == null ? null : data.copyTag();
  }

  /**
   * Gets a string variant name for the given stack
   * @param stack  Stack instance to check
   * @return  String variant name
   */
  public static String getSubtype(ItemStack stack) {
    Fluid fluid = getFluid(stack);
    return fluid == Fluids.EMPTY ? "" : BuiltInRegistries.FLUID.getKey(fluid).toString();
  }
}
