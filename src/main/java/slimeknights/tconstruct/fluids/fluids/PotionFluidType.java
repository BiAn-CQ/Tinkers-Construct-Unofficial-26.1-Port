package slimeknights.tconstruct.fluids.fluids;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.tconstruct.fluids.TinkerFluids;

import java.util.Objects;
import java.util.List;
import java.util.Optional;

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
      removeLegacyPotionData(itemStack);
    }
    return itemStack;
  }

  /** Creates the potion tag */
  private static CompoundTag potionTag(Identifier location) {
    CompoundTag tag = new CompoundTag();
    tag.putString("Potion", location.toString());
    return tag;
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
    Identifier id = potion.unwrapKey().map(ResourceKey::identifier)
      .orElseGet(() -> BuiltInRegistries.POTION.getKey(potion.value()));
    return FluidOutput.fromTag(Objects.requireNonNull(TinkerFluids.potion.getCommonTag()), size, potionTag(id));
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

  /**
   * Gets potion contents from a fluid stack, accepting the legacy {@code Potion}
   * custom-data key still used by tag-preference recipe outputs.
   */
  public static PotionContents getPotionContents(FluidStack stack) {
    return mergeLegacyPotionData(
      stack.get(DataComponents.POTION_CONTENTS),
      stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
  }

  /** Gets potion contents from an item stack, including legacy mod-item custom data. */
  public static PotionContents getPotionContents(ItemStack stack) {
    return mergeLegacyPotionData(
      stack.get(DataComponents.POTION_CONTENTS),
      stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
  }

  private static PotionContents mergeLegacyPotionData(PotionContents contents, CompoundTag legacy) {
    Optional<Integer> customColor = legacy.contains("CustomPotionColor")
      ? Optional.of(legacy.getIntOr("CustomPotionColor", PotionContents.BASE_POTION_COLOR))
      : Optional.empty();
    if (contents != null) {
      if (contents.customColor().isEmpty() && customColor.isPresent()) {
        return new PotionContents(contents.potion(), customColor, contents.customEffects(), contents.customName());
      }
      return contents;
    }
    Identifier id = Identifier.tryParse(legacy.getStringOr("Potion", ""));
    Optional<Holder<Potion>> potion = id == null
      ? Optional.empty()
      : BuiltInRegistries.POTION.get(id).map(holder -> holder);
    if (potion.isPresent() || customColor.isPresent()) {
      return new PotionContents(potion, customColor, List.of(), Optional.empty());
    }
    return PotionContents.EMPTY;
  }

  /** Removes potion fields that have already been converted to the native item component. */
  public static void removeLegacyPotionData(ItemStack stack) {
    CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
    if (customData == null) {
      return;
    }
    CompoundTag remaining = customData.copyTag();
    remaining.remove("Potion");
    remaining.remove("CustomPotionColor");
    if (remaining.isEmpty()) {
      stack.remove(DataComponents.CUSTOM_DATA);
    } else {
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(remaining));
    }
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
