package slimeknights.tconstruct.fluids.fluids;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.tconstruct.fluids.TinkerFluids;


public class PotionFluidType extends FluidType {
  public PotionFluidType(Properties properties) {
    super(properties);
  }

  @Override
  public String getDescriptionId(FluidStack stack) {
    Holder<Potion> potion = getPotion(stack);
    return "item.minecraft.potion.effect." + potion.value().name();
  }

  @Override
  public ItemStack getBucket(FluidStack fluidStack) {
    ItemStack itemStack = new ItemStack(fluidStack.getFluid().getBucket());
    if (!fluidStack.isComponentsPatchEmpty()) {
      itemStack.applyComponents(fluidStack.getComponentsPatch());
    }
    PotionContents contents = getPotionContents(fluidStack);
    if (!contents.equals(PotionContents.EMPTY)) {
      itemStack.set(DataComponents.POTION_CONTENTS, contents);
    }
    return itemStack;
  }

  /** Creates a fluid stack for the given potion */
  public static FluidStack potionFluid(ResourceKey<Potion> potion, int size) {
    Holder.Reference<Potion> holder = BuiltInRegistries.POTION.get(potion.identifier()).orElseGet(
      () -> BuiltInRegistries.POTION.get(Identifier.withDefaultNamespace("water")).orElseThrow());
    return potionFluid(holder, size);
  }

  /** Creates a fluid stack for the given potion holder. */
  public static FluidStack potionFluid(Holder<Potion> potion, int size) {
    FluidStack stack = new FluidStack(TinkerFluids.potion.get(), size);
    stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
    return stack;
  }

  /** Creates a fluid stack for the given potion */
  @SuppressWarnings("deprecation")  // forge registries have nullable keys, like why would you want that?
  public static FluidStack potionFluid(Potion potion, int size) {
    return potionFluid(BuiltInRegistries.POTION.wrapAsHolder(potion), size);
  }

  /** Creates a fluid output for the given potion */
  public static FluidOutput potionResult(Holder<Potion> potion, int size) {
    return FluidOutput.fromStack(potionFluid(potion, size));
  }

  /** Creates a fluid output for the given potion */
  @SuppressWarnings("deprecation")  // forge registries have nullable keys, like why would you want that?
  public static FluidOutput potionResult(Potion potion, int size) {
    return potionResult(BuiltInRegistries.POTION.wrapAsHolder(potion), size);
  }

  /** Creates a potion bucket for the given potion */
  public static ItemStack potionBucket(ResourceKey<Potion> potion) {
    Holder.Reference<Potion> holder = BuiltInRegistries.POTION.get(potion.identifier()).orElseGet(
      () -> BuiltInRegistries.POTION.get(Identifier.withDefaultNamespace("water")).orElseThrow());
    return potionBucket(holder);
  }

  /** Creates a potion bucket for the given potion holder. */
  public static ItemStack potionBucket(Holder<Potion> potion) {
    ItemStack stack = new ItemStack(TinkerFluids.potion);
    stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
    return stack;
  }

  /** Gets native potion contents from a fluid stack. */
  public static PotionContents getPotionContents(FluidStack stack) {
    return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
  }

  /** Gets native potion contents from an item stack. */
  public static PotionContents getPotionContents(ItemStack stack) {
    return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
  }

  /** Gets the native potion holder carried by a fluid stack, defaulting to water. */
  public static Holder<Potion> getPotion(FluidStack stack) {
    return getPotionContents(stack).potion().orElse(Potions.WATER);
  }

  /** Creates a potion bucket for the given potion */
  @SuppressWarnings("deprecation")  // forge registries have nullable keys, like why would you want that?
  public static ItemStack potionBucket(Potion potion) {
    return potionBucket(BuiltInRegistries.POTION.wrapAsHolder(potion));
  }
}
