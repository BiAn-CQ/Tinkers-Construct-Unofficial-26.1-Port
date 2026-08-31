package slimeknights.tconstruct.shared;

import com.mojang.serialization.MapCodec;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TintedGlassBlock;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import slimeknights.mantle.data.predicate.block.BlockPredicate;
import slimeknights.mantle.data.predicate.damage.DamageSourcePredicate;
import slimeknights.mantle.data.predicate.entity.LivingEntityPredicate;
import slimeknights.mantle.data.predicate.fluid.FluidPredicate;
import slimeknights.mantle.data.predicate.item.ItemPredicate;
import slimeknights.mantle.recipe.condition.TagFilledCondition;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.common.json.BlockOrEntityCondition;
import slimeknights.tconstruct.common.json.ConfigEnabledCondition;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator;
import slimeknights.tconstruct.gadgets.TinkerGadgets;
import slimeknights.tconstruct.library.json.condition.TagDifferencePresentCondition;
import slimeknights.tconstruct.library.json.condition.TagIntersectionPresentCondition;
import slimeknights.tconstruct.library.json.condition.LegacyItemExistsCondition;
import slimeknights.tconstruct.library.json.condition.TagNotEmptyCondition;
import slimeknights.tconstruct.library.json.loot.HasLootContextSetCondition;
import slimeknights.tconstruct.library.json.loot.TagPreferenceLootEntry;
import slimeknights.tconstruct.library.json.predicate.BlockAtFeetEntityPredicate;
import slimeknights.tconstruct.library.json.predicate.BlockVariableRangePredicate;
import slimeknights.tconstruct.library.json.predicate.EntityVariableRangePredicate;
import slimeknights.tconstruct.library.json.predicate.HarvestTierPredicate;
import slimeknights.tconstruct.library.json.predicate.HasMobEffectPredicate;
import slimeknights.tconstruct.library.json.predicate.TinkerPredicate;
import slimeknights.tconstruct.library.recipe.ingredient.BlockTagIngredient;
import slimeknights.tconstruct.library.recipe.ingredient.InstrumentIngredient;
import slimeknights.tconstruct.library.recipe.ingredient.NoContainerIngredient;
import slimeknights.tconstruct.library.utils.SlimeBounceHandler;
import slimeknights.tconstruct.shared.block.BetterPaneBlock;
import slimeknights.tconstruct.shared.block.ClearGlassPaneBlock;
import slimeknights.tconstruct.shared.block.ClearStainedGlassBlock;
import slimeknights.tconstruct.shared.block.ClearStainedGlassBlock.GlassColor;
import slimeknights.tconstruct.shared.block.ClearStainedGlassPaneBlock;
import slimeknights.tconstruct.shared.block.GlowBlock;
import slimeknights.tconstruct.shared.block.PlatformBlock;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.shared.block.SoulGlassBlock;
import slimeknights.tconstruct.shared.block.SoulGlassPaneBlock;
import slimeknights.tconstruct.shared.block.WaxedPlatformBlock;
import slimeknights.tconstruct.shared.block.WeatheringPlatformBlock;
import slimeknights.tconstruct.shared.command.TConstructCommand;
import slimeknights.tconstruct.shared.inventory.BlockContainerOpenedTrigger;
import slimeknights.tconstruct.shared.item.CheeseBlockItem;
import slimeknights.tconstruct.shared.item.CheeseItem;
import slimeknights.tconstruct.shared.item.TinkerBookItem;
import slimeknights.tconstruct.shared.item.TinkerBookItem.BookType;
import slimeknights.tconstruct.shared.particle.FluidParticleData;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tools.TinkerModifiers;

import static slimeknights.tconstruct.TConstruct.getResource;

/**
 * Contains items and blocks and stuff that is shared by multiple modules, but might be required individually
 */
@SuppressWarnings("unused")
public final class TinkerCommons extends TinkerModule {
  /** Creative tab for general items, or those that lack another tab */
  public static final DeferredHolder<CreativeModeTab, ? extends CreativeModeTab> tabGeneral = CREATIVE_TABS.register(
    "general", () -> CreativeModeTab.builder().title(TConstruct.makeTranslation("itemGroup", "general"))
                                    .icon(() -> new ItemStack(TinkerCommons.materialsAndYou))
                                    .displayItems(TinkerCommons::addTabItems)
                                    .build());

  /*
   * Blocks
   */
  public static final ItemObject<GlowBlock> glowBlock = BLOCKS.register("glow", () -> new GlowBlock(builder(MapColor.NONE, SoundType.WOOL).noCollision().pushReaction(PushReaction.DESTROY).replaceable().strength(0.0F).lightLevel(s -> 14).noOcclusion()), BLOCK_ITEM);
  /**
   * @deprecated Use {@link #glowBlock}
   */
  @Deprecated(forRemoval = true)
  public static final DeferredHolder<Block, ? extends GlowBlock> glow = DeferredHolder.<Block,GlowBlock>create(Registries.BLOCK, glowBlock.getId());
  // glass
  public static final ItemObject<TransparentBlock> clearGlass = BLOCKS.register("clear_glass", () -> new TransparentBlock(glassBuilder(MapColor.NONE)), BLOCK_ITEM);
  public static final ItemObject<TintedGlassBlock> clearTintedGlass = BLOCKS.register("clear_tinted_glass", () -> new TintedGlassBlock(glassBuilder(MapColor.COLOR_GRAY).noOcclusion().isValidSpawn((state, level, pos, entity) -> false).isRedstoneConductor((state, level, pos) -> false).isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false)), BLOCK_ITEM);
  public static final ItemObject<ClearGlassPaneBlock> clearGlassPane = BLOCKS.register("clear_glass_pane", () -> new ClearGlassPaneBlock(glassBuilder(MapColor.NONE)), BLOCK_ITEM);
  public static final EnumObject<GlassColor,ClearStainedGlassBlock> clearStainedGlass = BLOCKS.registerEnum(GlassColor.values(), "clear_stained_glass", (color) -> new ClearStainedGlassBlock(glassBuilder(color.getDye().getMapColor()), color), BLOCK_ITEM);
  public static final EnumObject<GlassColor,ClearStainedGlassPaneBlock> clearStainedGlassPane = BLOCKS.registerEnum(GlassColor.values(), "clear_stained_glass_pane", (color) -> new ClearStainedGlassPaneBlock(glassBuilder(color.getDye().getMapColor()), color), BLOCK_ITEM);
  public static final ItemObject<SoulGlassBlock> soulGlass = BLOCKS.register("soul_glass", () -> new SoulGlassBlock(glassBuilder(MapColor.COLOR_BROWN).speedFactor(0.2F).noCollision().forceSolidOn().isViewBlocking((state, getter, pos) -> true)), TOOLTIP_BLOCK_ITEM);
  public static final ItemObject<ClearGlassPaneBlock> soulGlassPane = BLOCKS.register("soul_glass_pane", () -> new SoulGlassPaneBlock(glassBuilder(MapColor.COLOR_BROWN).speedFactor(0.2F)), TOOLTIP_BLOCK_ITEM);
  // panes
  public static final ItemObject<IronBarsBlock> goldBars = BLOCKS.register("gold_bars", () -> new IronBarsBlock(builder(MapColor.NONE, SoundType.METAL).requiresCorrectToolForDrops().strength(3.0F, 6.0F).noOcclusion()), TOOLTIP_BLOCK_ITEM);
  public static final ItemObject<BetterPaneBlock> obsidianPane = BLOCKS.register("obsidian_pane", () -> new BetterPaneBlock(builder(MapColor.COLOR_BLACK, SoundType.STONE).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM).noOcclusion().strength(25.0F, 400.0F)), BLOCK_ITEM);
  // platforms
  public static final ItemObject<PlatformBlock> goldPlatform = BLOCKS.register("gold_platform", () -> new PlatformBlock(builder(MapColor.GOLD, SoundType.COPPER).requiresCorrectToolForDrops().strength(3.0F, 6.0F).noOcclusion()), TOOLTIP_BLOCK_ITEM);
  public static final ItemObject<PlatformBlock> ironPlatform = BLOCKS.register("iron_platform", () -> new PlatformBlock(builder(MapColor.METAL, SoundType.COPPER).requiresCorrectToolForDrops().strength(5.0F, 6.0F).noOcclusion()), BLOCK_ITEM);
  public static final ItemObject<PlatformBlock> cobaltPlatform = BLOCKS.register("cobalt_platform", () -> new PlatformBlock(builder(MapColor.COLOR_BLUE, SoundType.COPPER).requiresCorrectToolForDrops().strength(5.0f).noOcclusion()), BLOCK_ITEM);
  public static final EnumObject<WeatherState,PlatformBlock> copperPlatform = new EnumObject.Builder<WeatherState,PlatformBlock>(WeatherState.class)
    .put(WeatherState.UNAFFECTED, BLOCKS.register("copper_platform",           () -> new WeatheringPlatformBlock(WeatherState.UNAFFECTED, builder(MapColor.COLOR_ORANGE,          SoundType.COPPER).requiresCorrectToolForDrops().strength(3.0F, 6.0F).noOcclusion()), BLOCK_ITEM))
    .put(WeatherState.EXPOSED,    BLOCKS.register("exposed_copper_platform",   () -> new WeatheringPlatformBlock(WeatherState.EXPOSED,    builder(MapColor.TERRACOTTA_LIGHT_GRAY, SoundType.COPPER).requiresCorrectToolForDrops().strength(3.0F, 6.0F).noOcclusion()), BLOCK_ITEM))
    .put(WeatherState.WEATHERED,  BLOCKS.register("weathered_copper_platform", () -> new WeatheringPlatformBlock(WeatherState.WEATHERED,  builder(MapColor.WARPED_STEM,           SoundType.COPPER).requiresCorrectToolForDrops().strength(3.0F, 6.0F).noOcclusion()), BLOCK_ITEM))
    .put(WeatherState.OXIDIZED,   BLOCKS.register("oxidized_copper_platform",  () -> new WeatheringPlatformBlock(WeatherState.OXIDIZED,   builder(MapColor.WARPED_NYLIUM,         SoundType.COPPER).requiresCorrectToolForDrops().strength(3.0F, 6.0F).noOcclusion()), BLOCK_ITEM))
    .build();
  public static final EnumObject<WeatherState,PlatformBlock> waxedCopperPlatform = new EnumObject.Builder<WeatherState,PlatformBlock>(WeatherState.class)
    .put(WeatherState.UNAFFECTED, BLOCKS.register("waxed_copper_platform",           () -> new WaxedPlatformBlock(WeatherState.UNAFFECTED, builder(MapColor.COLOR_ORANGE,          SoundType.COPPER).requiresCorrectToolForDrops().strength(3.0F, 6.0F).noOcclusion()), BLOCK_ITEM))
    .put(WeatherState.EXPOSED,    BLOCKS.register("waxed_exposed_copper_platform",   () -> new WaxedPlatformBlock(WeatherState.EXPOSED,    builder(MapColor.TERRACOTTA_LIGHT_GRAY, SoundType.COPPER).requiresCorrectToolForDrops().strength(3.0F, 6.0F).noOcclusion()), BLOCK_ITEM))
    .put(WeatherState.WEATHERED,  BLOCKS.register("waxed_weathered_copper_platform", () -> new WaxedPlatformBlock(WeatherState.WEATHERED,  builder(MapColor.WARPED_STEM,           SoundType.COPPER).requiresCorrectToolForDrops().strength(3.0F, 6.0F).noOcclusion()), BLOCK_ITEM))
    .put(WeatherState.OXIDIZED,   BLOCKS.register("waxed_oxidized_copper_platform",  () -> new WaxedPlatformBlock(WeatherState.OXIDIZED,   builder(MapColor.WARPED_NYLIUM,         SoundType.COPPER).requiresCorrectToolForDrops().strength(3.0F, 6.0F).noOcclusion()), BLOCK_ITEM))
    .build();


  /*
   * Items
   */
  public static final ItemObject<Item> bacon = ITEMS.register("bacon", () -> new Item(itemProperties().food(TinkerFood.BACON.food(), TinkerFood.BACON.consumable())));
  public static final ItemObject<Item> jeweledApple = ITEMS.register("jeweled_apple", () -> new Item(itemProperties().food(TinkerFood.JEWELED_APPLE.food(), TinkerFood.JEWELED_APPLE.consumable())));
  public static final ItemObject<Item> cheeseIngot = ITEMS.register("cheese_ingot", () -> new CheeseItem(itemProperties().food(TinkerFood.CHEESE.food(), TinkerFood.CHEESE.consumable())));
  public static final ItemObject<Block> cheeseBlock = BLOCKS.register("cheese_block", () -> new HalfTransparentBlock(builder(MapColor.COLOR_YELLOW, SoundType.HONEY_BLOCK).strength(1.5F, 3.0F).speedFactor(0.4F).jumpFactor(0.5F).noOcclusion()), block -> new CheeseBlockItem(block, itemProperties().food(TinkerFood.CHEESE.food(), TinkerFood.CHEESE.consumable())));

  public static final ItemObject<TinkerBookItem> materialsAndYou  = ITEMS.register("materials_and_you", () -> new TinkerBookItem(itemProperties(UNSTACKABLE_PROPS), BookType.MATERIALS_AND_YOU));
  public static final ItemObject<TinkerBookItem> punySmelting     = ITEMS.register("puny_smelting",     () -> new TinkerBookItem(itemProperties(UNSTACKABLE_PROPS), BookType.PUNY_SMELTING));
  public static final ItemObject<TinkerBookItem> mightySmelting   = ITEMS.register("mighty_smelting",   () -> new TinkerBookItem(itemProperties(UNSTACKABLE_PROPS), BookType.MIGHTY_SMELTING));
  public static final ItemObject<TinkerBookItem> tinkersGadgetry  = ITEMS.register("tinkers_gadgetry",  () -> new TinkerBookItem(itemProperties(UNSTACKABLE_PROPS), BookType.TINKERS_GADGETRY));
  public static final ItemObject<TinkerBookItem> fantasticFoundry = ITEMS.register("fantastic_foundry", () -> new TinkerBookItem(itemProperties(UNSTACKABLE_PROPS), BookType.FANTASTIC_FOUNDRY));
  public static final ItemObject<TinkerBookItem> encyclopedia     = ITEMS.register("encyclopedia",      () -> new TinkerBookItem(itemProperties(UNSTACKABLE_PROPS), BookType.ENCYCLOPEDIA));

  public static final DeferredHolder<ParticleType<?>, ? extends ParticleType<FluidParticleData>> fluidParticle = PARTICLE_TYPES.register("fluid", FluidParticleData.Type::new);

  /* Loot conditions */
  public static final DeferredHolder<MapCodec<? extends LootItemCondition>, MapCodec<? extends LootItemCondition>> lootConfig = LOOT_CONDITIONS.register(ConfigEnabledCondition.ID.getPath(), () -> ConfigEnabledCondition.CODEC);
  public static final DeferredHolder<MapCodec<? extends LootItemCondition>, MapCodec<? extends LootItemCondition>> lootBlockOrEntity = LOOT_CONDITIONS.register("block_or_entity", () -> BlockOrEntityCondition.CODEC);
  public static final DeferredHolder<MapCodec<? extends LootItemCondition>, MapCodec<? extends LootItemCondition>> hasLootContextSet = LOOT_CONDITIONS.register("has_context_set", () -> HasLootContextSetCondition.CODEC);
  /** @deprecated use {@link slimeknights.mantle.loot.MantleLoot#TAG_FILLED} */
  @SuppressWarnings("removal")
  @Deprecated(forRemoval = true)
  public static final DeferredHolder<MapCodec<? extends LootItemCondition>, MapCodec<? extends LootItemCondition>> lootTagNotEmptyCondition = LOOT_CONDITIONS.register("tag_not_empty", () -> TagNotEmptyCondition.CODEC);
  /** @deprecated use {@link slimeknights.mantle.loot.MantleLoot#TAG_PREFERENCE} */
  @SuppressWarnings("removal")
  @Deprecated(forRemoval = true)
  public static final DeferredHolder<MapCodec<? extends LootPoolEntryContainer>, MapCodec<? extends LootPoolEntryContainer>> lootTagPreference = LOOT_ENTRIES.register("tag_preference", () -> TagPreferenceLootEntry.CODEC);

  /* Slime Balls are edible, believe it or not */
  public static final EnumObject<SlimeType, Item> slimeball = new EnumObject.Builder<SlimeType, Item>(SlimeType.class)
    .put(SlimeType.EARTH, () -> Items.SLIME_BALL)
    .putAll(ITEMS.registerEnum(SlimeType.TINKER, "slime_ball", type -> new Item(itemProperties(ITEM_PROPS))))
    .build();

  public static final BlockContainerOpenedTrigger CONTAINER_OPENED_TRIGGER = new BlockContainerOpenedTrigger();

  public TinkerCommons() {
    TConstructCommand.init();
    NeoForge.EVENT_BUS.addListener(RecipeCacheInvalidator::onReloadListenerReload);
  }

  @SubscribeEvent
  void commonSetupEvent(FMLCommonSetupEvent event) {
    SlimeBounceHandler.init();
  }

  @SuppressWarnings("removal")
  @SubscribeEvent
  void registerRecipeSerializers(RegisterEvent event) {
    if (event.getRegistryKey() == Registries.RECIPE_SERIALIZER) {
      CriteriaTriggers.register(TConstruct.MOD_ID + ":block_container_opened", CONTAINER_OPENED_TRIGGER);
      // mantle
      DamageSourcePredicate.LOADER.register(getResource("direct"), TinkerPredicate.DIRECT_DAMAGE.getLoader());
      // entity
      LivingEntityPredicate.LOADER.register(getResource("airborne"), TinkerPredicate.AIRBORNE.getLoader());
      LivingEntityPredicate.LOADER.register(getResource("targeting_block"), TinkerPredicate.TARGETING_BLOCK.getLoader());
      LivingEntityPredicate.LOADER.register(getResource("full_health"), TinkerPredicate.FULL_HEALTH.getLoader());
      LivingEntityPredicate.LOADER.register(getResource("variable_range"), EntityVariableRangePredicate.LOADER);
      LivingEntityPredicate.LOADER.register(getResource("has_effect"), HasMobEffectPredicate.LOADER);
      LivingEntityPredicate.LOADER.register(getResource("block_at_feet"), BlockAtFeetEntityPredicate.LOADER);
      // item
      ItemPredicate.LOADER.register(getResource("arrow"), TinkerPredicate.ARROW.getLoader());
      ItemPredicate.LOADER.register(getResource("bucket"), TinkerPredicate.BUCKET.getLoader());
      ItemPredicate.LOADER.register(getResource("map"), TinkerPredicate.MAP.getLoader());
      ItemPredicate.LOADER.register(getResource("can_melt"), TinkerPredicate.CAN_MELT_ITEM.getLoader());
      ItemPredicate.LOADER.register(getResource("castable"), TinkerPredicate.CASTABLE.getLoader());
      // block
      BlockPredicate.LOADER.register(getResource("blocks_motion"), TinkerPredicate.BLOCKS_MOTION.getLoader());
      BlockPredicate.LOADER.register(getResource("can_be_replaced"), TinkerPredicate.CAN_BE_REPLACED.getLoader());
      BlockPredicate.LOADER.register(getResource("bush"), TinkerPredicate.BUSH.getLoader());
      BlockPredicate.LOADER.register(getResource("can_melt"), TinkerPredicate.CAN_MELT_BLOCK.getLoader());
      BlockPredicate.LOADER.register(getResource("harvest_tier"), HarvestTierPredicate.LOADER);
      BlockPredicate.LOADER.register(getResource("variable_range"), BlockVariableRangePredicate.LOADER);
      // fluid
      FluidPredicate.LOADER.register(getResource("fuel"), TinkerPredicate.FUEL.getLoader());
    } else if (event.getRegistryKey() == net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.CONDITION_CODECS) {
      event.register(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.CONDITION_CODECS, helper -> {
        helper.register(TagIntersectionPresentCondition.ID, TagIntersectionPresentCondition.CODEC);
        helper.register(TagDifferencePresentCondition.ID, TagDifferencePresentCondition.CODEC);
        // Legacy Tinkers books use the old loot-condition ID. Mantle's
        // TagFilledCondition has the same tag-non-empty semantics and also
        // implements NeoForge's ICondition used by book loading.
        helper.register(getResource("tag_not_empty"), legacyConditionCodec(TagFilledCondition.CODEC));
        // item_exists has a legacy field layout ("item" rather than the
        // native registered condition's "value"), so keep one canonical
        // compatibility codec owned by TConstruct and alias old spellings to it.
        helper.register(LegacyItemExistsCondition.ID, LegacyItemExistsCondition.CODEC);
        helper.register(ConfigEnabledCondition.ID, ConfigEnabledCondition.CODEC);
      });
      var conditionCodecs = event.getRegistry(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.CONDITION_CODECS);
      if (conditionCodecs != null) {
        Identifier neoAnd = Identifier.fromNamespaceAndPath("neoforge", "and");
        Identifier neoModLoaded = Identifier.fromNamespaceAndPath("neoforge", "mod_loaded");
        Identifier neoNever = Identifier.fromNamespaceAndPath("neoforge", "never");
        Identifier neoNot = Identifier.fromNamespaceAndPath("neoforge", "not");
        Identifier neoOr = Identifier.fromNamespaceAndPath("neoforge", "or");
        Identifier neoAlways = Identifier.fromNamespaceAndPath("neoforge", "always");
        conditionCodecs.addAlias(Identifier.fromNamespaceAndPath("forge", "and"), neoAnd);
        conditionCodecs.addAlias(Identifier.fromNamespaceAndPath("forge", "mod_loaded"), neoModLoaded);
        conditionCodecs.addAlias(Identifier.fromNamespaceAndPath("forge", "never"), neoNever);
        conditionCodecs.addAlias(Identifier.fromNamespaceAndPath("forge", "not"), neoNot);
        conditionCodecs.addAlias(Identifier.fromNamespaceAndPath("forge", "or"), neoOr);
        conditionCodecs.addAlias(Identifier.fromNamespaceAndPath("forge", "always"), neoAlways);
        conditionCodecs.addAlias(Identifier.fromNamespaceAndPath("forge", "true"), neoAlways);
        conditionCodecs.addAlias(Identifier.fromNamespaceAndPath("forge", "false"), neoNever);
        conditionCodecs.addAlias(Identifier.fromNamespaceAndPath("forge", "item_exists"), LegacyItemExistsCondition.ID);
        conditionCodecs.addAlias(Identifier.fromNamespaceAndPath("neoforge", "true"), neoAlways);
        conditionCodecs.addAlias(Identifier.fromNamespaceAndPath("neoforge", "item_exists"), LegacyItemExistsCondition.ID);
      }
    } else if (event.getRegistryKey() == net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.INGREDIENT_TYPES) {
      event.register(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.INGREDIENT_TYPES, helper ->
        {
          helper.register(getResource("block_tag"), BlockTagIngredient.TYPE);
          helper.register(getResource("instrument"), InstrumentIngredient.TYPE);
          helper.register(NoContainerIngredient.ID, NoContainerIngredient.TYPE);
        });
    }
  }

  /** Wraps a built-in codec so the registry can expose a legacy alias without a duplicate value. */
  private static <T extends net.neoforged.neoforge.common.conditions.ICondition> MapCodec<T> legacyConditionCodec(MapCodec<T> codec) {
    return codec.xmap(value -> value, value -> value);
  }

  /** Adds all relevant items to the creative tab */
  private static void addTabItems(ItemDisplayParameters itemDisplayParameters, CreativeModeTab.Output output) {
    // tables
    TinkerTables.addTabItems(itemDisplayParameters, output);

    // books
    output.accept(materialsAndYou);
    output.accept(punySmelting);
    output.accept(mightySmelting);
    output.accept(tinkersGadgetry);
    output.accept(fantasticFoundry);
    output.accept(encyclopedia);

    // food
    output.accept(bacon);
    output.accept(jeweledApple);
    output.accept(cheeseIngot);
    output.accept(cheeseBlock);

    // glass
    output.accept(clearGlass);
    accept(output, clearStainedGlass);
    output.accept(clearTintedGlass);
    output.accept(soulGlass);
    output.accept(clearGlassPane);
    accept(output, clearStainedGlassPane);
    output.accept(soulGlassPane);
    // bars
    output.accept(goldBars);
    output.accept(obsidianPane);
    // platforms
    accept(output, copperPlatform);
    accept(output, waxedCopperPlatform);
    output.accept(ironPlatform);
    output.accept(goldPlatform);
    output.accept(cobaltPlatform);

    // slimeballs are in world

    TinkerGadgets.addTabItems(itemDisplayParameters, output);
    TinkerMaterials.addTabItems(itemDisplayParameters, output);
    TinkerModifiers.addTabItems(itemDisplayParameters, output);
  }
}
