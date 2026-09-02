package slimeknights.tconstruct.library.tools.definition;

import net.minecraft.world.entity.EquipmentSlot;

import java.util.Locale;

/** The four armor slots used by modifiable armor definitions and data generators. */
public enum ArmorSlotType {
  HELMET(EquipmentSlot.HEAD),
  CHESTPLATE(EquipmentSlot.CHEST),
  LEGGINGS(EquipmentSlot.LEGS),
  BOOTS(EquipmentSlot.FEET);

  private final EquipmentSlot equipmentSlot;

  ArmorSlotType(EquipmentSlot equipmentSlot) {
    this.equipmentSlot = equipmentSlot;
  }

  public String getName() {
    return name().toLowerCase(Locale.ROOT);
  }

  public EquipmentSlot getEquipmentSlot() {
    return equipmentSlot;
  }
}
