package slimeknights.tconstruct.plugin.jsonthings;

import dev.gigaherz.jsonthings.things.serializers.FlexItemType;
import dev.gigaherz.jsonthings.things.serializers.IItemSerializer;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.neoforged.neoforge.common.util.Lazy;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.tools.definition.ArmorSlotType;
import slimeknights.tconstruct.library.json.TinkerLoadables;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.item.ModifiableArrowItem;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.item.ModifiableShurikenItem;
import slimeknights.tconstruct.library.tools.item.armor.DummyArmorMaterial;
import slimeknights.tconstruct.library.tools.item.armor.ModifiableArmorItem;
import slimeknights.tconstruct.library.tools.item.armor.MultilayerArmorItem;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableBowItem;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableCrossbowItem;
import slimeknights.tconstruct.library.tools.part.ToolPartItem;
import slimeknights.tconstruct.plugin.jsonthings.item.FlexPartCastItem;
import slimeknights.tconstruct.plugin.jsonthings.item.IMaterialItemFactory;
import slimeknights.tconstruct.plugin.jsonthings.item.IToolItemFactory;
import slimeknights.tconstruct.tools.item.ModifiableSwordItem;
import slimeknights.tconstruct.tools.item.RepairKitItem;

/** Collection of custom Json Things item types added by Tinkers. */
@SuppressWarnings("unused")
public final class FlexItemTypes {
  private FlexItemTypes() {}

  public static void init() {
    register("tool_part", data -> {
      MaterialStatsId statType = new MaterialStatsId(
        Identifier.parse(GsonHelper.getAsString(data, "stat_type")));
      return (IMaterialItemFactory<ToolPartItem>)
        (properties, builder) -> new ToolPartItem(properties, statType);
    });

    register("repair_kit", data -> {
      float repairAmount = GsonHelper.getAsFloat(data, "repair_amount");
      return (IMaterialItemFactory<RepairKitItem>)
        (properties, builder) -> new RepairKitItem(properties, repairAmount);
    });

    register("tool", data -> {
      boolean breakBlocksInCreative = GsonHelper.getAsBoolean(data, "break_blocks_in_creative", true);
      int stackSize = GsonHelper.getAsInt(data, "max_stack_size", 1);
      return (IToolItemFactory<ModifiableItem>) (properties, builder) -> {
        properties.stacksTo(stackSize);
        ToolDefinition definition = ToolDefinition.create(builder.getRegistryName());
        return breakBlocksInCreative
          ? new ModifiableItem(properties, definition, stackSize)
          : new ModifiableSwordItem(properties, definition, stackSize);
      };
    });

    register("bow", data -> {
      boolean storeDrawingItem = GsonHelper.getAsBoolean(data, "store_drawing_item", false);
      return (IToolItemFactory<ModifiableBowItem>) (properties, builder) ->
        new ModifiableBowItem(properties, ToolDefinition.create(builder.getRegistryName()), storeDrawingItem);
    });

    register("crossbow", data -> {
      boolean allowFireworks = GsonHelper.getAsBoolean(data, "allow_fireworks");
      boolean storeDrawingItem = GsonHelper.getAsBoolean(data, "store_drawing_item", false);
      return (IToolItemFactory<ModifiableCrossbowItem>) (properties, builder) ->
        new ModifiableCrossbowItem(properties, ToolDefinition.create(builder.getRegistryName()),
          allowFireworks ? ProjectileWeaponItem.ARROW_OR_FIREWORK : ProjectileWeaponItem.ARROW_ONLY,
          storeDrawingItem);
    });

    register("arrow", data -> (IToolItemFactory<ModifiableArrowItem>)
      (properties, builder) -> new ModifiableArrowItem(
        properties, ToolDefinition.create(builder.getRegistryName())));

    register("shuriken", data -> (IToolItemFactory<ModifiableShurikenItem>)
      (properties, builder) -> new ModifiableShurikenItem(
        properties, ToolDefinition.create(builder.getRegistryName())));

    register("part_cast", data -> {
      Identifier partId = Identifier.parse(GsonHelper.getAsString(data, "part"));
      return (properties, builder) -> new FlexPartCastItem(
        properties, builder, Lazy.of(() -> Loadables.ITEM.fromKey(partId, "part")));
    });

    register("basic_armor", data -> {
      Identifier name = Identifier.parse(GsonHelper.getAsString(data, "texture_name"));
      SoundEvent sound = Loadables.SOUND_EVENT.getOrDefault(
        data, "equip_sound", SoundEvents.ARMOR_EQUIP_GENERIC.value());
      ArmorSlotType slot = TinkerLoadables.ARMOR_SLOT.getIfPresent(data, "slot");
      return (IToolItemFactory<ModifiableArmorItem>) (properties, builder) -> {
        DummyArmorMaterial material = new DummyArmorMaterial(name, sound);
        return new ModifiableArmorItem(material.asArmorMaterial(), slot, properties,
          ToolDefinition.create(builder.getRegistryName()));
      };
    });

    register("multilayer_armor", data -> {
      Identifier name = Identifier.parse(GsonHelper.getAsString(data, "model_name"));
      SoundEvent sound = Loadables.SOUND_EVENT.getOrDefault(
        data, "equip_sound", SoundEvents.ARMOR_EQUIP_GENERIC.value());
      ArmorSlotType slot = TinkerLoadables.ARMOR_SLOT.getIfPresent(data, "slot");
      return (IToolItemFactory<MultilayerArmorItem>) (properties, builder) -> {
        ToolDefinition definition = ToolDefinition.create(builder.getRegistryName());
        DummyArmorMaterial material = new DummyArmorMaterial(name, sound);
        return new MultilayerArmorItem(
          material.asArmorMaterial(), slot, properties, definition, name);
      };
    });
  }

  private static <T extends Item> void register(String name, IItemSerializer<T> serializer) {
    FlexItemType.register(TConstruct.resourceString(name), serializer);
  }
}
