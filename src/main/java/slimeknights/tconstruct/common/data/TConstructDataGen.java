package slimeknights.tconstruct.common.data;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.PackOutput;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.data.loot.GlobalLootModifiersProvider;
import slimeknights.tconstruct.common.data.loot.LootTableInjectionProvider;
import slimeknights.tconstruct.common.data.loot.TConstructLootTableProvider;
import slimeknights.tconstruct.common.data.render.RenderFluidProvider;
import slimeknights.tconstruct.common.data.render.RenderItemProvider;
import slimeknights.tconstruct.common.data.tags.BiomeTagProvider;
import slimeknights.tconstruct.common.data.tags.BlockEntityTypeTagProvider;
import slimeknights.tconstruct.common.data.tags.BlockTagProvider;
import slimeknights.tconstruct.common.data.tags.DamageTypeTagProvider;
import slimeknights.tconstruct.common.data.tags.EnchantmentTagProvider;
import slimeknights.tconstruct.common.data.tags.EntityTypeTagProvider;
import slimeknights.tconstruct.common.data.tags.FluidTagProvider;
import slimeknights.tconstruct.common.data.tags.ItemTagProvider;
import slimeknights.tconstruct.common.data.tags.InstrumentTagProvider;
import slimeknights.tconstruct.common.data.tags.MaterialTagProvider;
import slimeknights.tconstruct.common.data.tags.MenuTypeTagProvider;
import slimeknights.tconstruct.common.data.tags.ModifierTagProvider;
import slimeknights.tconstruct.common.data.tags.PotionTagProvider;
import slimeknights.tconstruct.fluids.data.FluidBlockstateModelProvider;
import slimeknights.tconstruct.fluids.data.FluidBucketModelProvider;
import slimeknights.tconstruct.fluids.data.FluidTextureProvider;
import slimeknights.tconstruct.fluids.data.FluidTooltipProvider;
import slimeknights.tconstruct.gadgets.data.GadgetRecipeProvider;
import slimeknights.tconstruct.shared.data.CommonRecipeProvider;
import slimeknights.tconstruct.smeltery.data.FluidContainerTransferProvider;
import slimeknights.tconstruct.smeltery.data.SmelteryRecipeProvider;
import slimeknights.tconstruct.tables.data.TableRecipeProvider;
import slimeknights.tconstruct.tools.data.EnchantmentToModifierProvider;
import slimeknights.tconstruct.tools.data.FluidEffectProvider;
import slimeknights.tconstruct.tools.data.ModifierProvider;
import slimeknights.tconstruct.tools.data.ModifierRecipeProvider;
import slimeknights.tconstruct.tools.data.StationSlotLayoutProvider;
import slimeknights.tconstruct.tools.data.ToolDefinitionDataProvider;
import slimeknights.tconstruct.tools.data.ToolItemModelProvider;
import slimeknights.tconstruct.tools.data.ToolsRecipeProvider;
import slimeknights.tconstruct.tools.data.material.MaterialDataProvider;
import slimeknights.tconstruct.tools.data.material.MaterialRecipeProvider;
import slimeknights.tconstruct.tools.data.material.MaterialRenderInfoProvider;
import slimeknights.tconstruct.tools.data.material.MaterialStatsDataProvider;
import slimeknights.tconstruct.tools.data.material.MaterialTraitsDataProvider;
import slimeknights.tconstruct.tools.data.material.TrimMaterialProvider;
import slimeknights.tconstruct.tools.data.sprite.TinkerMaterialSpriteProvider;
import slimeknights.tconstruct.tools.data.sprite.TinkerPartSpriteProvider;
import slimeknights.tconstruct.tools.data.sprite.TinkerTrimMaterialPaletteGenerator;
import slimeknights.tconstruct.tools.data.client.ModifierModelMapProvider;
import slimeknights.tconstruct.library.client.data.ClientDataSerializers;
import slimeknights.tconstruct.library.client.data.material.GeneratorPartTextureJsonGenerator;
import slimeknights.tconstruct.library.client.data.material.MaterialPaletteDebugGenerator;
import slimeknights.tconstruct.library.client.data.material.MaterialPartTextureGenerator;
import slimeknights.tconstruct.world.data.MobEquipmentProvider;
import slimeknights.tconstruct.world.data.AncientToolTradeProvider;
import slimeknights.tconstruct.world.data.StructureRepalleter;
import slimeknights.tconstruct.world.data.WorldRecipeProvider;
import slimeknights.tconstruct.world.data.WorldgenProvider;

/** Registers the 26.1 split client/server data-generation events. */
public final class TConstructDataGen {
  private TConstructDataGen() {}

  public static void init(IEventBus bus) {
    bus.addListener(TConstructDataGen::gatherServerData);
    bus.addListener(TConstructDataGen::gatherClientData);
  }

  private static void gatherServerData(GatherDataEvent.Server event) {
    RegistrySetBuilder registries = new RegistrySetBuilder();
    DamageTypeProvider.register(registries);
    WorldgenProvider.register(registries);
    TrimMaterialProvider.register(registries);
    event.createDatapackRegistryObjects(registries);

    event.createBlockAndItemTags(BlockTagProvider::new, ItemTagProvider::new);
    event.createProvider(FluidTagProvider::new);
    event.createProvider(EntityTypeTagProvider::new);
    event.createProvider(BlockEntityTypeTagProvider::new);
    event.createProvider(BiomeTagProvider::new);
    event.createProvider(EnchantmentTagProvider::new);
    event.createProvider(MenuTypeTagProvider::new);
    event.createProvider(PotionTagProvider::new);
    event.createProvider(DamageTypeTagProvider::new);
    event.createProvider(InstrumentTagProvider::new);

    event.createProvider(TConstructLootTableProvider::new);
    event.createProvider(AdvancementsProvider::new);
    event.createProvider(GlobalLootModifiersProvider::new);
    event.createProvider(LootTableInjectionProvider::new);
    event.createProvider(ConfigurationDataProvider::new);
    event.createProvider(AncientToolTradeProvider::new);

    ResourceManager resources = event.getResourceManager(PackType.SERVER_DATA);
    PackOutput output = event.getGenerator().getPackOutput();
    event.addProvider(new StructureRepalleter(output, resources));
    event.addProvider(new MaterialTagProvider(output, resources));
    event.addProvider(new ModifierTagProvider(output, resources));
    event.createProvider(MobEquipmentProvider::new);
    event.createProvider(FluidContainerTransferProvider::new);

    MaterialDataProvider materials = event.createProvider(MaterialDataProvider::new);
    event.addProvider(new MaterialStatsDataProvider(output, materials));
    event.addProvider(new MaterialTraitsDataProvider(output, materials));
    event.createProvider(ToolDefinitionDataProvider::new);
    event.createProvider(StationSlotLayoutProvider::new);
    event.createProvider(EnchantmentToModifierProvider::new);
    event.createProvider(ModifierProvider::new);
    event.createProvider(FluidEffectProvider::new);

    event.addProvider(BaseRecipeProvider.runner(output, event.getLookupProvider(), "common recipes", CommonRecipeProvider::new));
    event.addProvider(BaseRecipeProvider.runner(output, event.getLookupProvider(), "world recipes", WorldRecipeProvider::new));
    event.addProvider(BaseRecipeProvider.runner(output, event.getLookupProvider(), "gadget recipes", GadgetRecipeProvider::new));
    event.addProvider(BaseRecipeProvider.runner(output, event.getLookupProvider(), "table recipes", TableRecipeProvider::new));
    event.addProvider(BaseRecipeProvider.runner(output, event.getLookupProvider(), "smeltery recipes", SmelteryRecipeProvider::new));
    event.addProvider(BaseRecipeProvider.runner(output, event.getLookupProvider(), "material recipes", MaterialRecipeProvider::new));
    event.addProvider(BaseRecipeProvider.runner(output, event.getLookupProvider(), "tool recipes", ToolsRecipeProvider::new));
    event.addProvider(BaseRecipeProvider.runner(output, event.getLookupProvider(), "modifier recipes", ModifierRecipeProvider::new));
  }

  private static void gatherClientData(GatherDataEvent.Client event) {
    ClientDataSerializers.initialize();
    event.createProvider(RenderFluidProvider::new);
    event.createProvider(RenderItemProvider::new);
    event.createProvider(FluidTooltipProvider::new);
    event.createProvider(FluidTextureProvider::new);
    event.createProvider(output -> new FluidBucketModelProvider(output, "tconstruct"));
    event.createProvider(output -> new FluidBlockstateModelProvider(output, "tconstruct"));

    ResourceManager resources = event.getResourceManager(PackType.CLIENT_RESOURCES);
    PackOutput output = event.getGenerator().getPackOutput();
    TinkerMaterialSpriteProvider materialSprites = new TinkerMaterialSpriteProvider();
    TinkerPartSpriteProvider partSprites = new TinkerPartSpriteProvider();
    event.addProvider(new ToolItemModelProvider(output, resources));
    event.addProvider(new MaterialRenderInfoProvider(output, materialSprites, resources));
    event.addProvider(new GeneratorPartTextureJsonGenerator(output, TConstruct.MOD_ID, partSprites));
    event.addProvider(new MaterialPartTextureGenerator(output, resources, partSprites, materialSprites));
    event.addProvider(new MaterialPaletteDebugGenerator(output, TConstruct.MOD_ID, materialSprites));
    event.addProvider(new TinkerTrimMaterialPaletteGenerator(output, resources, materialSprites));
    event.addProvider(new ModifierModelMapProvider(output));
  }
}
