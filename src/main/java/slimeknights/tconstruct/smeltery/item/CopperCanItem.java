package slimeknights.tconstruct.smeltery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import java.util.List;
import java.util.function.Consumer;

/**
 * Fluid container holding 1 ingot of fluid
 */
public class CopperCanItem extends Item {
  public CopperCanItem(Properties properties) {
    super(properties);
  }

  public ResourceHandler<FluidResource> getFluidHandler(ItemAccess itemAccess) {
    return new CopperCanFluidHandler(itemAccess);
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
    FluidStack fluid = getFluidStack(stack);
    if (!fluid.isEmpty()) {
      MutableComponent text = fluid.getHoverName().plainCopy();
      lines.add(Component.translatable(this.getDescriptionId() + ".contents", text).withStyle(ChatFormatting.GRAY));
      if (flag.isAdvanced()) {
        lines.add(Component.translatable(TankItem.FLUID_ID, Loadables.FLUID.getKey(fluid.getFluid())).withStyle(ChatFormatting.DARK_GRAY));
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
    return getFluidStack(stack).getFluid();
  }

  /** Gets a copy of the complete fluid stack stored in the can. */
  public static FluidStack getFluidStack(ItemStack stack) {
    SimpleFluidContent fluid = stack.get(TinkerModule.FLUID_STACK_COMPONENT.get());
    return fluid == null || fluid.isEmpty() ? FluidStack.EMPTY : fluid.copy();
  }

  /** Adds filled variants of the copper can to the given consumer */
  @SuppressWarnings("deprecation")
  public static void addFilledVariants(Consumer<ItemStack> output) {
    for (Fluid fluid : BuiltInRegistries.FLUID) {
      var holder = fluid.builtInRegistryHolder();
      if (fluid.isSource(fluid.defaultFluidState()) && !holder.is(TinkerTags.Fluids.HIDE_IN_CREATIVE_TANKS)) {
        output.accept(CopperCanItem.setFluid(new ItemStack(TinkerSmeltery.copperCan), new FluidStack(fluid, FluidValues.INGOT)));
      }
    }
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
