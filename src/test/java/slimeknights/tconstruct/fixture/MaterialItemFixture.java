package slimeknights.tconstruct.fixture;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import slimeknights.tconstruct.library.tools.part.ToolPartItem;
import slimeknights.tconstruct.test.CoreTestBootstrap;
import slimeknights.tconstruct.tools.stats.HandleMaterialStats;
import slimeknights.tconstruct.tools.stats.HeadMaterialStats;
import slimeknights.tconstruct.tools.stats.StatlessMaterialStats;

public class MaterialItemFixture {

  public static ToolPartItem MATERIAL_ITEM, MATERIAL_ITEM_2, MATERIAL_ITEM_HEAD, MATERIAL_ITEM_HANDLE, MATERIAL_ITEM_EXTRA;

  private MaterialItemFixture() {
  }

  private static boolean init = false;
  @SuppressWarnings({"unchecked", "deprecation"}) // isolated mutation of the built-in registry for synthetic test parts
  public static void init() {
    if (init) {
      return;
    }
    ((MappedRegistry<Item>)BuiltInRegistries.ITEM).unfreeze(false);
    ResourceKey<Item> materialKey = key("test_material");
    ResourceKey<Item> material2Key = key("test_material_2");
    ResourceKey<Item> headKey = key("test_head");
    ResourceKey<Item> handleKey = key("test_handle");
    ResourceKey<Item> extraKey = key("test_extra");
    MATERIAL_ITEM = new ToolPartItem(new Item.Properties().setId(materialKey), MaterialStatsFixture.STATS_TYPE);
    MATERIAL_ITEM_2 = new ToolPartItem(new Item.Properties().setId(material2Key), MaterialStatsFixture.STATS_TYPE_2);
    MATERIAL_ITEM_HEAD = new ToolPartItem(new Item.Properties().setId(headKey), HeadMaterialStats.ID);
    MATERIAL_ITEM_HANDLE = new ToolPartItem(new Item.Properties().setId(handleKey), HandleMaterialStats.ID);
    MATERIAL_ITEM_EXTRA = new ToolPartItem(new Item.Properties().setId(extraKey), StatlessMaterialStats.BINDING.getIdentifier());
    Registry.register(BuiltInRegistries.ITEM, materialKey, MATERIAL_ITEM);
    Registry.register(BuiltInRegistries.ITEM, material2Key, MATERIAL_ITEM_2);
    Registry.register(BuiltInRegistries.ITEM, headKey, MATERIAL_ITEM_HEAD);
    Registry.register(BuiltInRegistries.ITEM, handleKey, MATERIAL_ITEM_HANDLE);
    Registry.register(BuiltInRegistries.ITEM, extraKey, MATERIAL_ITEM_EXTRA);
    CoreTestBootstrap.bindMissingItemComponents();
    init = true;
  }

  private static ResourceKey<Item> key(String path) {
    return ResourceKey.create(BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("test", path));
  }
}
