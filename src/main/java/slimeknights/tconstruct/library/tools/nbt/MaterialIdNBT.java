package slimeknights.tconstruct.library.tools.nbt;

import com.google.common.collect.ImmutableList;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.materials.IMaterialRegistry;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.utils.ItemStackDataUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Similar to {@link slimeknights.tconstruct.library.tools.nbt.MaterialNBT}, but does not check materials against the registry.
 * Used for rendering so we can have client side only materials for UIs. Anything logic based should use the regular material NBT
 */
@EqualsAndHashCode
@ToString
public class MaterialIdNBT {
  /** Instance containing no materials, for errors with parsing NBT */
  public final static MaterialIdNBT EMPTY = new MaterialIdNBT(ImmutableList.of());

  /** List of materials contained in this NBT */
  @Getter
  private final List<MaterialVariantId> materials;

  /** Creates a new material NBT */
  public MaterialIdNBT(List<? extends MaterialVariantId> materials) {
    this.materials = List.copyOf(materials);
  }

  /** Gets the number of materials on this stack. Note this may not match the number of materials the tool desires. */
  public int size() {
    return materials.size();
  }

  /**
   * Gets the material at the given index
   * @param index  Index
   * @return  Material, or unknown if index is invalid
   * @see #getMaterial(ItemStack, int)
   */
  public MaterialVariantId getMaterial(int index) {
    if (index >= materials.size() || index < 0) {
      return MaterialId.UNKNOWN;
    }
    return materials.get(index);
  }

  /** Copies the list, padding missing entries when replacing a material. */
  public MaterialIdNBT replaceMaterial(int index, MaterialVariantId replacement) {
    if (index < 0) throw new IndexOutOfBoundsException("Material index is out of bounds");
    List<MaterialVariantId> result = new ArrayList<>(materials);
    while (result.size() <= index) result.add(MaterialId.UNKNOWN);
    result.set(index, replacement);
    return new MaterialIdNBT(result);
  }

  /** Resolves all redirects, replacing with material redirects */
  public MaterialIdNBT resolveRedirects() {
    boolean changed = false;
    ImmutableList.Builder<MaterialVariantId> builder = ImmutableList.builder();
    IMaterialRegistry registry = MaterialRegistry.getInstance();
    for (MaterialVariantId id : materials) {
      MaterialId original = id.getId();
      MaterialId resolved = registry.resolve(original);
      if (resolved != original) {
        changed = true;
      }
      builder.add(MaterialVariantId.create(resolved, id.getVariant()));
    }
    // return a new instance only if things changed
    if (changed) {
      return new MaterialIdNBT(builder.build());
    }
    return this;
  }

  /** Tries to parse the tag as a material variant ID, returning unknown if invalid. */
  private static MaterialVariantId tryParse(String tag) {
    MaterialVariantId material = MaterialVariantId.tryParse(tag);
    if (material != null) {
      return material;
    }
    return MaterialId.UNKNOWN;
  }

  /**
   * Creates a copy of the given materials. Used for recipe viewers to ensure the materials displayed is craftable.
   * @param keep            Number of materials to keep
   * @param extraMaterials  Extra materials to add after those kept
   * @return  Updated materials list.
   */
  public MaterialIdNBT normalize(int keep, List<MaterialVariantId> extraMaterials) {
    // no work to do if the size is already fine
    if (extraMaterials.isEmpty() && size() <= keep) {
      return this;
    }
    List<MaterialVariantId> list = new ArrayList<>(keep + extraMaterials.size());
    for (int i = 0; i < keep; i++) {
      list.add(materials.get(i));
    }
    list.addAll(extraMaterials);
    return new MaterialIdNBT(list);
  }

  /**
   * Parses the material list from NBT
   * @param nbt  NBT instance
   * @return  MaterialNBT instance
   */
  public static MaterialIdNBT readFromNBT(@Nullable Tag nbt) {
    if (nbt == null || nbt.getId() != Tag.TAG_LIST) {
      return EMPTY;
    }
    ListTag listNBT = (ListTag) nbt;
    if (listNBT.stream().anyMatch(tag -> tag.getId() != Tag.TAG_STRING)) {
      return EMPTY;
    }

    List<MaterialVariantId> materials = listNBT.stream()
      .map(tag -> tag.asString().orElse(""))
      .map(MaterialIdNBT::tryParse)
      .collect(java.util.stream.Collectors.toList());
    return new MaterialIdNBT(materials);
  }

  /**
   * Writes this material list to NBT
   * @return  List of materials
   */
  public ListTag serializeToNBT() {
    ListTag list = new ListTag();
    for (MaterialVariantId material : materials) {
      list.add(StringTag.valueOf(material.toString()));
    }
    return list;
  }

  /**
   * Parses the material list from a stack
   * @param stack  Tool stack instance
   * @return  MaterialNBT instance
   */
  public static MaterialIdNBT from(ItemStack stack) {
    CompoundTag nbt = ItemStackDataUtil.getTag(stack);
    if (nbt != null) {
      return readFromNBT(nbt.getListOrEmpty(ToolStack.TAG_MATERIALS));
    }
    return EMPTY;
  }

  /** Helper to quickly fetch a single material ID from a stack. Use {@link #from(ItemStack)} and {@link #getMaterial(int)} instead if you need to parse multiple. */
  public static MaterialVariantId getMaterial(ItemStack stack, int index) {
    CompoundTag nbt = ItemStackDataUtil.getTag(stack);
    if (nbt != null) {
      ListTag list = nbt.getListOrEmpty(ToolStack.TAG_MATERIALS);
      if (index < list.size()) {
        return tryParse(list.getString(index).orElse(""));
      }
    }
    return MaterialId.UNKNOWN;
  }

  /** Writes this material list to the given stack */
  public ItemStack updateStack(ItemStack stack) {
    ItemStackDataUtil.updateTag(stack, tag -> tag.put(ToolStack.TAG_MATERIALS, serializeToNBT()));
    return stack;
  }

  /** Writes this material list to the given stack */
  @SuppressWarnings("UnusedReturnValue")
  public CompoundTag updateNBT(CompoundTag nbt) {
    nbt.put(ToolStack.TAG_MATERIALS, serializeToNBT());
    return nbt;
  }
}
