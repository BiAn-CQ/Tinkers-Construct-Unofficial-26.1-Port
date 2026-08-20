package slimeknights.tconstruct.library.tools.item.armor;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.EquipmentAssets;
import slimeknights.mantle.registration.object.IdAwareObject;

import java.util.Map;

/** Armor material that returns 0 except for name, since we bypass all the usages */
public class DummyArmorMaterial implements IdAwareObject {
  private final Identifier id;
  private final SoundEvent equipSound;
  private Holder<ArmorMaterial> holder;

  public DummyArmorMaterial(Identifier id, SoundEvent equipSound) {
    this.id = id;
    this.equipSound = equipSound;
  }

  @Override
  public Identifier getId() {
    return id;
  }

  /** Gets the vanilla armor material holder required by 1.21 armor items. */
  public Holder<ArmorMaterial> asArmorMaterial() {
    if (holder == null) {
      holder = Holder.direct(new ArmorMaterial(
        1,
        Map.of(),
        0,
        Holder.direct(equipSound),
        0,
        0,
        ItemTags.REPAIRS_LEATHER_ARMOR,
        ResourceKey.create(EquipmentAssets.ROOT_ID, id)
      ));
    }
    return holder;
  }
}
