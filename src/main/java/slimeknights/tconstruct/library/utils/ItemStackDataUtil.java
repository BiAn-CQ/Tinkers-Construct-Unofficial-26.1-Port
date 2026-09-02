package slimeknights.tconstruct.library.utils;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/** Helpers for the item custom-data component. */
public final class ItemStackDataUtil {
  private ItemStackDataUtil() {}

  /** Copies a stack with a new count while preserving all data components. */
  public static ItemStack copyStackWithSize(ItemStack stack, int count) {
    return stack.copyWithCount(count);
  }

  /** Gets a mutable copy of the stack's custom data, or {@code null} if absent. */
  @Nullable
  public static CompoundTag getTag(ItemStack stack) {
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    return data == null ? null : data.copyTag();
  }

  /** Gets a mutable copy of the stack's custom data, creating an empty tag if absent. */
  public static CompoundTag getOrCreateTag(ItemStack stack) {
    CompoundTag tag = getTag(stack);
    return tag == null ? new CompoundTag() : tag;
  }

  /** Stores the given custom data back onto the stack. */
  public static void setTag(ItemStack stack, @Nullable CompoundTag tag) {
    if (tag == null || tag.isEmpty()) {
      stack.remove(DataComponents.CUSTOM_DATA);
    } else {
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
  }

  /** Mutates the stack's custom data and stores it back. */
  public static void updateTag(ItemStack stack, Consumer<CompoundTag> consumer) {
    CompoundTag tag = getOrCreateTag(stack);
    consumer.accept(tag);
    setTag(stack, tag);
  }

  /** Gets a child compound from the custom data. */
  @Nullable
  public static CompoundTag getTagElement(ItemStack stack, String key) {
    CompoundTag tag = getTag(stack);
    return tag != null && tag.contains(key) ? tag.getCompoundOrEmpty(key) : null;
  }

  /** Mutates a child compound inside the stack's custom data and stores it back. */
  public static void updateTagElement(ItemStack stack, String key, Consumer<CompoundTag> consumer) {
    CompoundTag tag = getOrCreateTag(stack);
    CompoundTag child = tag.contains(key) ? tag.getCompoundOrEmpty(key) : new CompoundTag();
    consumer.accept(child);
    if (child.isEmpty()) {
      tag.remove(key);
    } else {
      tag.put(key, child);
    }
    setTag(stack, tag);
  }

  /** Decodes an item stack from its 26.1 component codec representation. */
  public static ItemStack parse(HolderLookup.Provider provider, CompoundTag tag) {
    return ItemStack.OPTIONAL_CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), tag)
      .result().orElse(ItemStack.EMPTY);
  }

  /** Encodes an item stack into a new compound tag. */
  public static CompoundTag save(HolderLookup.Provider provider, ItemStack stack) {
    return save(provider, stack, new CompoundTag());
  }

  /** Encodes an item stack into the supplied compound tag. */
  public static CompoundTag save(HolderLookup.Provider provider, ItemStack stack, CompoundTag target) {
    Tag encoded = ItemStack.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), stack)
      .result().orElseGet(CompoundTag::new);
    if (encoded instanceof CompoundTag compound) {
      target.merge(compound);
    }
    return target;
  }
}
