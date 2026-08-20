package slimeknights.tconstruct.library.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.Holder;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.fluids.FluidInstance;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

import java.util.Optional;

/** Component-based helpers for fluid stacks that carried custom NBT before 1.21. */
public final class FluidStackDataUtil {
  private FluidStackDataUtil() {}

  /**
   * Persistent codec for an item fluid component that can also be decoded
   * during the initial server data reload. NeoForge's stock FluidStack codec
   * requires fluid default components to be bound, but those components are
   * not applied until after recipes and advancements finish loading.
   */
  public static final Codec<SimpleFluidContent> EARLY_SIMPLE_CONTENT_CODEC = ExtraCodecs.optionalEmptyMap(
    RecordCodecBuilder.<FluidStack>create(instance -> instance.group(
      FluidInstance.FLUID_HOLDER_CODEC.fieldOf(FluidInstance.FIELD_ID).forGetter(FluidStack::typeHolder),
      ExtraCodecs.POSITIVE_INT.fieldOf(FluidInstance.FIELD_AMOUNT).forGetter(FluidStack::getAmount),
      DataComponentPatch.CODEC.optionalFieldOf(FluidInstance.FIELD_COMPONENTS, DataComponentPatch.EMPTY)
        .forGetter(FluidStack::getComponentsPatch)
    ).apply(instance, FluidStackDataUtil::createDuringInitialReload))
  ).xmap(
    stack -> stack.map(SimpleFluidContent::copyOf).orElse(SimpleFluidContent.EMPTY),
    content -> content.isEmpty() ? Optional.empty() : Optional.of(content.copy())
  );

  /**
   * Makes the registry holder usable for the temporary stack decoded during
   * the first data reload. Pending default components have already been built
   * at this point and replace this empty map once the reload succeeds.
   */
  private static FluidStack createDuringInitialReload(Holder<Fluid> fluid, int amount, DataComponentPatch components) {
    if (!fluid.areComponentsBound() && fluid instanceof Holder.Reference<Fluid> reference) {
      reference.bindComponents(DataComponentMap.EMPTY);
    }
    return new FluidStack(fluid, amount, components);
  }

  public static FluidStack create(Fluid fluid, int amount, CompoundTag data) {
    FluidStack stack = new FluidStack(fluid, amount);
    if (data != null && !data.isEmpty()) {
      CompoundTag remaining = data.copy();
      String potionId = remaining.getStringOr("Potion", "");
      if (!potionId.isEmpty()) {
        Identifier id = Identifier.tryParse(potionId);
        if (id != null) {
          BuiltInRegistries.POTION.get(id).ifPresent(
            potion -> stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion)));
        }
        remaining.remove("Potion");
      }
      if (!remaining.isEmpty()) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(remaining));
      }
    }
    return stack;
  }

  /** Creates a stack with an exact copy of the supplied native data component patch. */
  public static FluidStack create(Fluid fluid, int amount, DataComponentPatch components) {
    return new FluidStack(BuiltInRegistries.FLUID.wrapAsHolder(fluid), amount, components);
  }

  /** Creates a fluid stack carrying the native potion component. */
  public static FluidStack createPotion(Fluid fluid, int amount, Holder<Potion> potion) {
    FluidStack stack = new FluidStack(fluid, amount);
    stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
    return stack;
  }

  /** Decodes the component-based fluid representation used by 26.1. */
  public static FluidStack parse(HolderLookup.Provider provider, CompoundTag tag) {
    return FluidStack.OPTIONAL_CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), tag)
      .result().orElse(FluidStack.EMPTY);
  }

  /** Encodes a fluid stack into a compound tag using the active registry context. */
  public static CompoundTag save(HolderLookup.Provider provider, FluidStack stack) {
    Tag encoded = FluidStack.OPTIONAL_CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), stack)
      .result().orElseGet(CompoundTag::new);
    return encoded instanceof CompoundTag compound ? compound : new CompoundTag();
  }
}
