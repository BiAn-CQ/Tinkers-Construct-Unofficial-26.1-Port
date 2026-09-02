package slimeknights.tconstruct.library.tools.item.armor;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import slimeknights.tconstruct.library.tools.definition.ArmorSlotType;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/** Armor model that applies multiple texture layers in order */
public class MultilayerArmorItem extends ModifiableArmorItem {
  private final Identifier name;
  public MultilayerArmorItem(ModifiableArmorMaterial material, ArmorSlotType slot, Properties properties) {
    this(material, slot, properties, material.getId());
  }

  public MultilayerArmorItem(ModifiableArmorMaterial material, ArmorSlotType slot, Properties properties, Identifier name) {
    super(material, slot, properties);
    this.name = name;
  }

  public MultilayerArmorItem(Holder<ArmorMaterial> material, ArmorSlotType slot, Properties properties, ToolDefinition toolDefinition, Identifier name) {
    this(material, slot, properties, toolDefinition, name,
      ResourceKey.create(EquipmentAssets.ROOT_ID, name));
  }

  public MultilayerArmorItem(Holder<ArmorMaterial> material, ArmorSlotType slot, Properties properties,
                             ToolDefinition toolDefinition, Identifier name, ResourceKey<EquipmentAsset> asset) {
    super(material, slot, properties, toolDefinition, asset);
    this.name = name;
  }

  public MultilayerArmorItem(ModifiableArmorMaterial material, ArmorSlotType slot, Properties properties, ToolDefinition toolDefinition, Identifier name) {
    this(material.asArmorMaterial(), slot, properties, toolDefinition, name);
  }

  /** Model identifier used by the data-driven armor renderer. */
  public Identifier getModelName() {
    return name;
  }

  public void initializeClient(Consumer<IClientItemExtensions> consumer) {
    consumer.accept(IClientItemExtensions.DEFAULT);
  }
}
