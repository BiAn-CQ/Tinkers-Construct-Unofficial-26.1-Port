package slimeknights.tconstruct.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterLoaders;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterSpriteSourcesEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.server.packs.PackType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import slimeknights.mantle.client.model.LegacyModelLoader;
import slimeknights.mantle.client.render.ChannelFluids;
import slimeknights.mantle.client.render.FaucetFluid;
import slimeknights.mantle.client.render.InventoryBlockEntityRenderer;
import slimeknights.mantle.client.render.InventoryRenderState;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.client.model.NativeTinkerItemModel;
import slimeknights.tconstruct.client.model.NativeFluidContainerItemModel;
import slimeknights.tconstruct.client.model.NativeModifierModelMapManager;
import slimeknights.tconstruct.client.model.NativeTinkerBlockStateModel;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.client.materials.MaterialTooltipCache;
import slimeknights.tconstruct.library.client.item.ModifiableCrossbowClientExtension;
import slimeknights.tconstruct.library.client.item.ModifiableItemClientExtension;
import slimeknights.tconstruct.library.client.book.content.AbstractMaterialContent;
import slimeknights.tconstruct.library.client.data.ClientDataSerializers;
import slimeknights.tconstruct.library.client.modifiers.ModifierIconManager;
import slimeknights.tconstruct.library.client.particle.AttackParticle;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableCrossbowItem;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableLauncherItem;
import slimeknights.tconstruct.library.utils.HarvestTiers;
import slimeknights.mantle.data.listener.ISafeManagerReloadListener;
import slimeknights.tconstruct.library.utils.DomainDisplayName;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.client.screen.AlloyerScreen;
import slimeknights.tconstruct.smeltery.client.screen.HeatingStructureScreen;
import slimeknights.tconstruct.smeltery.client.screen.MelterScreen;
import slimeknights.tconstruct.smeltery.client.screen.SingleItemScreenFactory;
import slimeknights.tconstruct.smeltery.client.render.TankBlockEntityRenderer;
import slimeknights.tconstruct.smeltery.client.render.TankInventoryBlockEntityRenderer;
import slimeknights.tconstruct.smeltery.client.render.GaugeBlockEntityRenderer;
import slimeknights.tconstruct.smeltery.client.render.ProxyTankBlockEntityRenderer;
import slimeknights.tconstruct.smeltery.client.render.FaucetBlockEntityRenderer;
import slimeknights.tconstruct.smeltery.client.render.ChannelBlockEntityRenderer;
import slimeknights.tconstruct.smeltery.client.render.CastingBlockEntityRenderer;
import slimeknights.tconstruct.smeltery.client.render.HeatingStructureBlockEntityRenderer;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.shared.block.entity.TableBlockEntity;
import slimeknights.tconstruct.tables.client.inventory.CraftingStationScreen;
import slimeknights.tconstruct.tables.client.inventory.PartBuilderScreen;
import slimeknights.tconstruct.tables.client.inventory.TinkerChestScreen;
import slimeknights.tconstruct.tables.client.inventory.TinkerStationScreen;
import slimeknights.tconstruct.tables.block.entity.chest.TinkersChestBlockEntity;
import slimeknights.tconstruct.tools.TinkerTools;
import slimeknights.tconstruct.tools.TinkerToolParts;
import slimeknights.tconstruct.tools.client.CrystalshotRenderer;
import slimeknights.tconstruct.tools.client.FluidEffectProjectileRenderer;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.client.material.ThrownShurikenRenderer;
import slimeknights.tconstruct.tools.client.material.ThrownToolRenderer;
import slimeknights.tconstruct.tools.entity.ModifiableArrow;
import slimeknights.tconstruct.tools.entity.ThrownTool;
import slimeknights.tconstruct.tools.client.ToolContainerScreen;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.shared.client.FluidParticle;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import slimeknights.mantle.fluid.texture.FluidTextureManager;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.tools.item.armor.MultilayerArmorItem;
import slimeknights.tconstruct.tools.clientcompat.ShieldBannerModifierSpriteSource;

/** Client registrations retained while the old model integrations are migrated. */
@EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT)
public final class TConstructClientCompat {
  private static final int POTION_FALLBACK_COLOR = 0xFFF800F8;
  private static final FluidTintSource POTION_TINT = new FluidTintSource() {
    @Override
    public int color(FluidState state) {
      return POTION_FALLBACK_COLOR;
    }

    @Override
    public int colorAsStack(FluidStack stack) {
      PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
      return contents == null ? POTION_FALLBACK_COLOR : 0xFF000000 | contents.getColor();
    }
  };

  private TConstructClientCompat() {}

  @SubscribeEvent
  static void initializeClient(FMLClientSetupEvent event) {
    ClientDataSerializers.initialize();
    FluidTextureManager.registerTintSource(TinkerFluids.potion.get().getFluidType(), POTION_TINT);
    // Register book page types and transformers before the first resource
    // reload.  Lazy initialization from the item use path is too late for
    // cached book data and can leave a valid book screen with empty pages.
    TConstructBookCompat.initialize();
    AbstractMaterialContent.registerFallbackPart(TinkerToolParts.fakeIngot);
    AbstractMaterialContent.registerFallbackPart(TinkerToolParts.fakeStorageBlockItem);
  }

  @SubscribeEvent
  static void registerClientExtensions(RegisterClientExtensionsEvent event) {
    Set<Item> specializedArmor = TConstructArmorClientExtensions.register(event);
    DynamicTableParticleExtensions.register(event);

    // NeoForge 26.1 no longer discovers the old per-item initializeClient
    // callback. Register the same extensions centrally so modifiable tools
    // retain Tinkers' first-person bow, crossbow, and throwing animations.
    List<Item> tools = new ArrayList<>();
    List<Item> crossbows = new ArrayList<>();
    BuiltInRegistries.ITEM.forEach(item -> {
      if (item instanceof ModifiableCrossbowItem) {
        crossbows.add(item);
      } else if (item instanceof ModifiableItem || item instanceof ModifiableLauncherItem) {
        tools.add(item);
      }
      if (item instanceof MultilayerArmorItem armor && !specializedArmor.contains(item)) {
        Identifier modelName = armor.getModelName();
        event.registerItem(new LegacyMultilayerArmorClientExtension(modelName), item);
      }
    });
    if (!tools.isEmpty()) {
      event.registerItem(ModifiableItemClientExtension.INSTANCE, tools.toArray(Item[]::new));
    }
    if (!crossbows.isEmpty()) {
      event.registerItem(ModifiableCrossbowClientExtension.INSTANCE, crossbows.toArray(Item[]::new));
    }
  }

  @SubscribeEvent
  static void registerClientReloadListeners(AddClientReloadListenersEvent event) {
    // The full TinkerClient class is still staged out of the 26.1 build. Keep
    // this small listener active so native material item models see render
    // information before the model manager bakes them.
    MaterialRenderInfoLoader.init(event);
    event.addListener(TConstruct.getResource("native_modifier_models"), NativeModifierModelMapManager.INSTANCE);
    ModifierIconManager.init(event);
    MaterialTooltipCache.init(event);
    DomainDisplayName.addResourceListener(event);
    FaucetFluid.initialize(event);
    ChannelFluids.initialize(event);
    event.addListener(TConstruct.getResource("harvest_tiers"), HarvestTiers.RELOAD_LISTENER);
    event.addListener(TConstruct.getResource("modifier_client_cache"), (ISafeManagerReloadListener) manager ->
      ModifierManager.INSTANCE.getAllValues().forEach(modifier -> modifier.clearCache(PackType.CLIENT_RESOURCES)));
    event.addListener(TConstruct.getResource("combat_fishing_hook_cache"),
      (ISafeManagerReloadListener) manager -> CombatFishingHookRendererCompat.clearCache());
    event.addListener(TConstruct.getResource("addon_armor_model_cache"),
      (ISafeManagerReloadListener) manager -> LegacyMultilayerArmorClientExtension.invalidateAll());
    // Jade 26.1 registers a listener for later changes to its translated-name
    // option, but does not apply the already-loaded value when registering it.
    // Synchronize that value after reload so creative-tab deduplication,
    // regular item tooltips, and custom JEI ingredients share one mod name.
    event.addListener(TConstruct.getResource("jade_mod_name_cache"),
      (ISafeManagerReloadListener) manager -> JadeModNameCacheCompat.synchronize());
  }

  @SubscribeEvent
  static void registerItemModels(RegisterItemModelsEvent event) {
    event.register(TConstruct.getResource("tool"), NativeTinkerItemModel.ToolUnbaked.MAP_CODEC);
    event.register(TConstruct.getResource("material"), NativeTinkerItemModel.MaterialUnbaked.MAP_CODEC);
    event.register(TConstruct.getResource("material_block"), NativeTinkerItemModel.MaterialBlockUnbaked.MAP_CODEC);
    event.register(TConstruct.getResource("tank"), NativeTinkerItemModel.TankUnbaked.MAP_CODEC);
    event.register(TConstruct.getResource("overrides"), NativeTinkerItemModel.LegacyOverridesUnbaked.MAP_CODEC);
    event.register(TConstruct.getResource("fluid_container"), NativeFluidContainerItemModel.Unbaked.MAP_CODEC);
  }

  @SubscribeEvent
  static void registerBlockStateModels(RegisterBlockStateModels event) {
    event.registerModel(TConstruct.getResource("tank"), NativeTinkerBlockStateModel.TankUnbaked.MAP_CODEC);
    event.registerModel(TConstruct.getResource("fluid_texture"), NativeTinkerBlockStateModel.FluidTextureUnbaked.MAP_CODEC);
    event.registerModel(TConstruct.getResource("material_block"), NativeTinkerBlockStateModel.MaterialBlockUnbaked.MAP_CODEC);
  }

  @SubscribeEvent
  static void registerModelLoaders(RegisterLoaders event) {
    LegacyModelLoader loader = LegacyModelLoader.INSTANCE;
    event.register(TConstruct.getResource("tool"), loader);
    event.register(TConstruct.getResource("material"), loader);
    event.register(TConstruct.getResource("material_block"), loader);
    event.register(TConstruct.getResource("fluid_container"), loader);
    event.register(TConstruct.getResource("fluid_texture"), loader);
    event.register(TConstruct.getResource("gui"), loader);
    event.register(TConstruct.getResource("tank"), loader);
  }

  @SubscribeEvent
  static void registerParticleFactories(RegisterParticleProvidersEvent event) {
    // CommonsClientEvents is still held out by the old GUI geometry loader;
    // keep the fluid particle registration active in the native 26.1 bridge.
    event.registerSpecial(TinkerCommons.fluidParticle.get(), new FluidParticle.Factory());
    event.registerSpriteSet(TinkerTools.hammerAttackParticle.get(), AttackParticle.Factory::new);
    event.registerSpriteSet(TinkerTools.axeAttackParticle.get(), AttackParticle.Factory::new);
    event.registerSpriteSet(TinkerTools.bonkAttackParticle.get(), AttackParticle.Factory::new);
  }

  @SubscribeEvent
  static void registerMenuScreens(RegisterMenuScreensEvent event) {
    event.register(TinkerSmeltery.melterContainer.get(), MelterScreen::new);
    event.register(TinkerSmeltery.alloyerContainer.get(), AlloyerScreen::new);
    event.register(TinkerSmeltery.smelteryContainer.get(), HeatingStructureScreen::new);
    event.register(TinkerSmeltery.singleItemContainer.get(), new SingleItemScreenFactory());
    event.register(TinkerTables.craftingStationContainer.get(), CraftingStationScreen::new);
    event.register(TinkerTables.tinkerStationContainer.get(), TinkerStationScreen::new);
    event.register(TinkerTables.partBuilderContainer.get(), PartBuilderScreen::new);
    event.register(TinkerTables.tinkerChestContainer.get(), TinkerChestScreen::new);
    event.register(TinkerTables.modifierWorktableContainer.get(), slimeknights.tconstruct.tables.client.inventory.ModifierWorktableScreen::new);
    event.register(TinkerTools.toolContainer.get(), ToolContainerScreen::new);
  }

  @SubscribeEvent
  static void registerBlockTints(RegisterColorHandlersEvent.BlockTintSources event) {
    event.register(List.of(new BlockTintSource() {
      @Override
      public int color(BlockState state) {
        return -1;
      }

      @Override
      public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof TinkersChestBlockEntity chest ? chest.getColor() : -1;
      }
    }), TinkerTables.tinkersChest.get());
  }

  /** Registers custom tint codecs referenced by native 26.1 item definitions. */
  @SubscribeEvent
  static void registerItemTintSources(RegisterColorHandlersEvent.ItemTintSources event) {
    event.register(TConstruct.getResource("modifier_crystal"), ModifierCrystalTintSource.MAP_CODEC);
  }

  @SubscribeEvent
  static void registerTableRenderers(EntityRenderersEvent.RegisterRenderers event) {
    BlockEntityRendererProvider<TableBlockEntity, InventoryRenderState> renderer = InventoryBlockEntityRenderer::new;
    event.registerBlockEntityRenderer(TinkerTables.craftingStationTile.get(), renderer);
    event.registerBlockEntityRenderer(TinkerTables.tinkerStationTile.get(), renderer);
    event.registerBlockEntityRenderer(TinkerTables.modifierWorktableTile.get(), renderer);
    event.registerBlockEntityRenderer(TinkerTables.partBuilderTile.get(), renderer);

    // First native 26.1 smeltery renderer slice. Keep the remaining legacy
    // renderers staged out until each one has an extracted render state.
    event.registerBlockEntityRenderer(TinkerSmeltery.tank.get(), TankBlockEntityRenderer::new);
    event.registerBlockEntityRenderer(TinkerSmeltery.alloyer.get(), TankBlockEntityRenderer::new);
    event.registerBlockEntityRenderer(TinkerSmeltery.fluidCannon.get(), context -> new TankInventoryBlockEntityRenderer<>(context, BlockStateProperties.FACING));
    event.registerBlockEntityRenderer(TinkerSmeltery.melter.get(), context -> new TankInventoryBlockEntityRenderer<>(context, BlockStateProperties.HORIZONTAL_FACING));
    event.registerBlockEntityRenderer(TinkerSmeltery.castingTank.get(), context -> new TankInventoryBlockEntityRenderer<>(context, BlockStateProperties.HORIZONTAL_FACING));
    event.registerBlockEntityRenderer(TinkerSmeltery.gauge.get(), GaugeBlockEntityRenderer::new);
    event.registerBlockEntityRenderer(TinkerSmeltery.proxyTank.get(), ProxyTankBlockEntityRenderer::new);
    event.registerBlockEntityRenderer(TinkerSmeltery.faucet.get(), FaucetBlockEntityRenderer::new);
    event.registerBlockEntityRenderer(TinkerSmeltery.channel.get(), ChannelBlockEntityRenderer::new);
    event.registerBlockEntityRenderer(TinkerSmeltery.table.get(), CastingBlockEntityRenderer::new);
    event.registerBlockEntityRenderer(TinkerSmeltery.basin.get(), CastingBlockEntityRenderer::new);
    event.registerBlockEntityRenderer(TinkerSmeltery.smeltery.get(), HeatingStructureBlockEntityRenderer::new);
    event.registerBlockEntityRenderer(TinkerSmeltery.foundry.get(), HeatingStructureBlockEntityRenderer::new);
  }

  @SubscribeEvent
  static void registerArmorLayers(EntityRenderersEvent.AddLayers event) {
    for (PlayerModelType modelType : event.getSkins()) {
      var playerRenderer = event.getPlayerRenderer(modelType);
      if (playerRenderer != null) {
        playerRenderer.addLayer(new TConstructArmorTrimLayer(playerRenderer, event.getEntityModels(), modelType == PlayerModelType.SLIM));
      }

      var mannequinRenderer = event.getMannequinRenderer(modelType);
      if (mannequinRenderer != null) {
        mannequinRenderer.addLayer(new TConstructArmorTrimLayer(mannequinRenderer, event.getEntityModels(), modelType == PlayerModelType.SLIM));
      }
    }
  }

  @SubscribeEvent
  static void registerCrystalshotRenderer(EntityRenderersEvent.RegisterRenderers event) {
    event.registerEntityRenderer(TinkerTools.indestructibleItem.get(), ItemEntityRenderer::new);
    event.registerEntityRenderer(TinkerTools.crystalshotEntity.get(), CrystalshotRenderer::new);
    event.registerEntityRenderer(TinkerTools.fishingHook.get(), CombatFishingHookRendererCompat::new);
    event.registerEntityRenderer(TinkerTools.materialArrow.get(), context -> new ThrownToolRenderer<ModifiableArrow>(context));
    event.registerEntityRenderer(TinkerTools.thrownShuriken.get(), ThrownShurikenRenderer::new);
    event.registerEntityRenderer(TinkerTools.thrownTool.get(), context -> new ThrownToolRenderer<ThrownTool>(context));
    event.registerEntityRenderer(TinkerModifiers.fluidSpitEntity.get(), FluidEffectProjectileRenderer::new);
    event.registerEntityRenderer(TinkerModifiers.fireball.get(), context -> new ThrownItemRenderer<>(context, 0.75f, true));
  }

  @SubscribeEvent
  static void registerSpriteSourceTypes(RegisterSpriteSourcesEvent event) {
    ShieldBannerModifierSpriteSource.register(event);
  }

}
