package slimeknights.tconstruct.library.compat;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.equipment.Equippable;

/** Compatibility view of the pre-26.1 armor item API. */
public class ArmorItem extends Item {
  public enum Type {
    HELMET, CHESTPLATE, LEGGINGS, BOOTS;

    public String getName() {
      return name().toLowerCase(java.util.Locale.ROOT);
    }
  }

  protected final Type type;
  protected final Holder<ArmorMaterial> material;

  public ArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
    this(material, type, properties, material.value().assetId());
  }

  /**
   * Creates armor with a specific 26.1 equipment asset.  Most Tinker armor
   * uses the material asset, while special items such as slime wings need a
   * separate asset so they participate in the WINGS layer without also
   * drawing the normal humanoid armor layers.
   */
  public ArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties,
                   ResourceKey<EquipmentAsset> asset) {
    super(properties.component(DataComponents.EQUIPPABLE, Equippable.builder(slotFor(type))
      .setEquipSound(material.value().equipSound())
      .setAsset(asset)
      .build()));
    this.material = material;
    this.type = type;
  }

  public Type getType() {
    return type;
  }

  public EquipmentSlot getEquipmentSlot() {
    return slotFor(type);
  }

  private static EquipmentSlot slotFor(Type type) {
    return switch (type) {
      case HELMET -> EquipmentSlot.HEAD;
      case CHESTPLATE -> EquipmentSlot.CHEST;
      case LEGGINGS -> EquipmentSlot.LEGS;
      case BOOTS -> EquipmentSlot.FEET;
    };
  }
}
