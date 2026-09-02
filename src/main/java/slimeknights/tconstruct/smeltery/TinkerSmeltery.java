package slimeknights.tconstruct.smeltery;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.MenuType;
import slimeknights.tconstruct.library.tools.definition.ArmorSlotType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.minecraft.world.item.CreativeModeTab.Output;
import net.minecraft.world.item.CreativeModeTab.TabVisibility;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockBehaviour.StatePredicate;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import slimeknights.mantle.block.GaugeBlock;
import slimeknights.mantle.fluid.transfer.FluidContainerTransferManager;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.mantle.registration.object.BuildingBlockObject;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.mantle.registration.object.FenceBuildingBlockObject;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.mantle.registration.object.WallBuildingBlockObject;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.registration.CastItemObject;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.fluids.item.EmptyPotionTransfer;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe;
import slimeknights.tconstruct.library.recipe.casting.CastDuplicationRecipe;
import slimeknights.tconstruct.library.recipe.casting.ItemCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.PotionCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.RetexturedCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.TipClearingCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.TippingCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.container.ContainerFillingRecipe;
import slimeknights.tconstruct.library.recipe.casting.material.CompositeCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.material.MaterialCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.material.MaterialFluidRecipe;
import slimeknights.tconstruct.library.recipe.casting.material.PartSwapCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.material.ToolCastingRecipe;
import slimeknights.tconstruct.library.recipe.entitymelting.EntityMeltingRecipe;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;
import slimeknights.tconstruct.library.recipe.melting.DamageableMeltingRecipe;
import slimeknights.tconstruct.library.recipe.melting.MaterialMeltingRecipe;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipe;
import slimeknights.tconstruct.library.recipe.melting.OreMeltingRecipe;
import slimeknights.tconstruct.library.recipe.molding.MoldingRecipe;
import slimeknights.tconstruct.library.tools.part.PartCastItem;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.shared.block.ClearGlassPaneBlock;
import slimeknights.tconstruct.shared.block.PlaceBlockDispenserBehavior;
import slimeknights.tconstruct.shared.block.SoulGlassPaneBlock;
import slimeknights.tconstruct.smeltery.block.CastingBasinBlock;
import slimeknights.tconstruct.smeltery.block.CastingTableBlock;
import slimeknights.tconstruct.smeltery.block.CastingTankBlock;
import slimeknights.tconstruct.smeltery.block.ChannelBlock;
import slimeknights.tconstruct.smeltery.block.FaucetBlock;
import slimeknights.tconstruct.smeltery.block.FluidCannonBlock;
import slimeknights.tconstruct.smeltery.block.KnightMetalFluidCannonBlock;
import slimeknights.tconstruct.smeltery.block.ProxyTankBlock;
import slimeknights.tconstruct.smeltery.block.RenderingGaugeBlock;
import slimeknights.tconstruct.smeltery.block.SearedLanternBlock;
import slimeknights.tconstruct.smeltery.block.component.RetexturedOrientableSmelteryBlock;
import slimeknights.tconstruct.smeltery.block.component.SearedBlock;
import slimeknights.tconstruct.smeltery.block.component.SearedDrainBlock;
import slimeknights.tconstruct.smeltery.block.component.SearedDuctBlock;
import slimeknights.tconstruct.smeltery.block.component.SearedGlassBlock;
import slimeknights.tconstruct.smeltery.block.component.SearedLadderBlock;
import slimeknights.tconstruct.smeltery.block.component.SearedPillarBlock;
import slimeknights.tconstruct.smeltery.block.component.SearedSoulGlassBlock;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock.TankType;
import slimeknights.tconstruct.smeltery.block.component.SearedTintedGlassBlock;
import slimeknights.tconstruct.smeltery.block.controller.AlloyerBlock;
import slimeknights.tconstruct.smeltery.block.controller.ControllerBlock;
import slimeknights.tconstruct.smeltery.block.controller.FoundryControllerBlock;
import slimeknights.tconstruct.smeltery.block.controller.HeaterBlock;
import slimeknights.tconstruct.smeltery.block.controller.MelterBlock;
import slimeknights.tconstruct.smeltery.block.controller.SmelteryControllerBlock;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.CastingTankBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.ChannelBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.FaucetBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.FluidCannonBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.GaugeBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.HeaterBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.LanternBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.ProxyTankBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.component.DrainBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.component.DuctBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.component.SmelteryComponentBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.component.SmelteryInputOutputBlockEntity.ChuteBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.component.SmelteryInputOutputBlockEntity.SmelteryFluidIO;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.AlloyerBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.FoundryBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.MelterBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.SmelteryBlockEntity;
import slimeknights.tconstruct.smeltery.item.CopperCanItem;
import slimeknights.tconstruct.smeltery.item.DummyMaterialItem;
import slimeknights.tconstruct.smeltery.item.TankItem;
import slimeknights.tconstruct.smeltery.menu.AlloyerContainerMenu;
import slimeknights.tconstruct.smeltery.menu.HeatingStructureContainerMenu;
import slimeknights.tconstruct.smeltery.menu.MelterContainerMenu;
import slimeknights.tconstruct.smeltery.menu.SingleItemContainerMenu;
import slimeknights.tconstruct.tools.TinkerToolParts;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static slimeknights.mantle.Mantle.commonResource;

/**
 * Contains logic for the multiblocks in the mod
 */
@SuppressWarnings("unused")
public final class TinkerSmeltery extends TinkerModule {
  /** Internal tank view used only by smeltery I/O blocks; controllers do not expose the standard fluid capability. */
  public static final BlockCapability<ResourceHandler<FluidResource>,net.minecraft.core.Direction> SMELTERY_TANK_CAPABILITY =
    BlockCapability.createSided(TConstruct.getResource("smeltery_internal_tank"), ResourceHandler.asClass());

  @SubscribeEvent
  void registerCapabilities(RegisterCapabilitiesEvent event) {
    event.registerBlockEntity(Capabilities.Fluid.BLOCK, tank.get(), (blockEntity, side) -> blockEntity.getTank());
    event.registerBlockEntity(Capabilities.Item.BLOCK, fluidCannon.get(), (blockEntity, side) -> blockEntity.getItemCapability());
    event.registerBlockEntity(Capabilities.Fluid.BLOCK, melter.get(), (blockEntity, side) -> blockEntity.getFluidCapability());
    event.registerBlockEntity(Capabilities.Item.BLOCK, melter.get(), (blockEntity, side) -> blockEntity.getItemCapability());
    event.registerBlockEntity(Capabilities.Fluid.BLOCK, alloyer.get(), (blockEntity, side) -> blockEntity.getFluidCapability());
    event.registerBlockEntity(Capabilities.Item.BLOCK, heater.get(), (blockEntity, side) -> blockEntity.getItemCapability());
    event.registerBlockEntity(Capabilities.Fluid.BLOCK, castingTank.get(), (blockEntity, side) -> blockEntity.getFluidCapability());
    event.registerBlockEntity(Capabilities.Fluid.BLOCK, basin.get(), (blockEntity, side) -> blockEntity.getTank());
    event.registerBlockEntity(Capabilities.Item.BLOCK, basin.get(), (blockEntity, side) -> blockEntity.getItemCapability());
    event.registerBlockEntity(Capabilities.Fluid.BLOCK, table.get(), (blockEntity, side) -> blockEntity.getTank());
    event.registerBlockEntity(Capabilities.Item.BLOCK, table.get(), (blockEntity, side) -> blockEntity.getItemCapability());
    event.registerBlockEntity(Capabilities.Item.BLOCK, smeltery.get(), (blockEntity, side) -> blockEntity.getItemCapability());
    event.registerBlockEntity(Capabilities.Item.BLOCK, foundry.get(), (blockEntity, side) -> blockEntity.getItemCapability());
    event.registerBlockEntity(SMELTERY_TANK_CAPABILITY, smeltery.get(), (blockEntity, side) -> blockEntity.getFluidCapability());
    event.registerBlockEntity(SMELTERY_TANK_CAPABILITY, foundry.get(), (blockEntity, side) -> blockEntity.getFluidCapability());
    event.registerBlockEntity(Capabilities.Item.BLOCK, proxyTank.get(), (blockEntity, side) -> blockEntity.getItemTank());
    event.registerBlockEntity(Capabilities.Fluid.BLOCK, proxyTank.get(), (blockEntity, side) -> blockEntity.getItemTank().getFluidHandler());
    event.registerBlockEntity(Capabilities.Fluid.BLOCK, channel.get(), (blockEntity, side) -> blockEntity.getFluidHandler(side));
    event.registerBlockEntity(Capabilities.Fluid.BLOCK, drain.get(), (blockEntity, side) -> blockEntity.getProxiedCapability(side));
    event.registerBlockEntity(Capabilities.Item.BLOCK, chute.get(), (blockEntity, side) -> blockEntity.getProxiedCapability(side));
    event.registerBlockEntity(Capabilities.Fluid.BLOCK, duct.get(), (blockEntity, side) -> blockEntity.getProxiedCapability(side));
    event.registerBlockEntity(Capabilities.Item.BLOCK, duct.get(), (blockEntity, side) -> blockEntity.getItemHandler());
    for (Item item : BuiltInRegistries.ITEM) {
      if (item instanceof TankItem tankItem) {
        event.registerItem(Capabilities.Fluid.ITEM, (stack, context) -> tankItem.getFluidHandler(context), item);
      } else if (item instanceof CopperCanItem canItem) {
        event.registerItem(Capabilities.Fluid.ITEM, (stack, context) -> canItem.getFluidHandler(context), item);
      }
    }
  }
  /** Predicate for something that never happens */
  private static final StatePredicate NEVER = (state, level, pos) -> false;
  /** Creative tab for smeltery, all contents related to the multiblocks */
  public static final DeferredHolder<CreativeModeTab, ? extends CreativeModeTab> tabSmeltery = CREATIVE_TABS.register(
    "smeltery", () -> CreativeModeTab.builder().title(TConstruct.makeTranslation("itemGroup", "smeltery"))
                                     .icon(() -> new ItemStack(TinkerSmeltery.smelteryController))
                                     .displayItems(TinkerSmeltery::addTabItems)
                                     .withTabsBefore(TinkerToolParts.tabToolParts.getId())
                                     .build());

  /* Bricks */
  /* Crafting related items */
  public static final ItemObject<Block> grout = BLOCKS.register("grout", builder(MapColor.COLOR_LIGHT_GRAY, SoundType.SAND).instrument(NoteBlockInstrument.SNARE).strength(3.0f).friction(0.8F), TOOLTIP_BLOCK_ITEM);
  public static final ItemObject<Block> netherGrout = BLOCKS.register("nether_grout", builder(MapColor.COLOR_BROWN, SoundType.SOUL_SOIL).instrument(NoteBlockInstrument.SNARE).strength(3.0f).friction(0.8F), TOOLTIP_BLOCK_ITEM);

  // seared blocks
  public static final BuildingBlockObject searedStone, searedPaver;
  public static final WallBuildingBlockObject searedCobble, searedBricks;
  public static final ItemObject<Block> searedCrackedBricks, searedFancyBricks, searedTriangleBricks;
  static {
    Supplier<Properties> properties = () -> searedSolidProps(1);
    Supplier<SearedBlock> searedBlock = () -> new SearedBlock(properties.get(), false);
    searedStone = BLOCKS.registerBuilding("seared_stone", searedBlock, TOOLTIP_BLOCK_ITEM);
    searedCobble = BLOCKS.registerWallBuilding("seared_cobble", searedBlock, TOOLTIP_BLOCK_ITEM);
    searedPaver = BLOCKS.registerBuilding("seared_paver", searedBlock, TOOLTIP_BLOCK_ITEM);
    searedBricks = BLOCKS.registerWallBuilding("seared_bricks", searedBlock, TOOLTIP_BLOCK_ITEM);
    searedCrackedBricks = BLOCKS.register("seared_cracked_bricks", searedBlock, TOOLTIP_BLOCK_ITEM);
    searedFancyBricks = BLOCKS.register("seared_fancy_bricks", searedBlock, TOOLTIP_BLOCK_ITEM);
    searedTriangleBricks = BLOCKS.register("seared_triangle_bricks", searedBlock, TOOLTIP_BLOCK_ITEM);
  }
  public static final ItemObject<Block> searedLamp = BLOCKS.register("seared_lamp", () -> new SearedBlock(searedSolidProps(1).lightLevel(state -> 15), false), TOOLTIP_BLOCK_ITEM);

  // scorched blocks
  public static final ItemObject<Block> scorchedStone, polishedScorchedStone, chiseledScorchedBricks;
  public static final FenceBuildingBlockObject scorchedBricks;
  public static final BuildingBlockObject scorchedRoad;
  static {
    Supplier<Properties> properties = () -> scorchedSolidProps(1);
    Supplier<SearedPillarBlock> pillar = () -> new SearedPillarBlock(properties.get(), false);
    scorchedStone = BLOCKS.register("scorched_stone", pillar, TOOLTIP_BLOCK_ITEM);
    polishedScorchedStone = BLOCKS.register("polished_scorched_stone", pillar, TOOLTIP_BLOCK_ITEM);
    Supplier<SearedBlock> block = () -> new SearedBlock(properties.get(), false);
    scorchedBricks = BLOCKS.registerFenceBuilding("scorched_bricks", block, TOOLTIP_BLOCK_ITEM);
    scorchedRoad = BLOCKS.registerBuilding("scorched_road", block, TOOLTIP_BLOCK_ITEM);
    chiseledScorchedBricks = BLOCKS.register("chiseled_scorched_bricks", block, TOOLTIP_BLOCK_ITEM);
  }
  public static final ItemObject<Block> scorchedLamp = BLOCKS.register("scorched_lamp", () -> new SearedBlock(scorchedSolidProps(1).lightLevel(state -> 15), false), TOOLTIP_BLOCK_ITEM);

  // glass
  public static final ItemObject<SearedGlassBlock> searedGlass, scorchedGlass;
  public static final ItemObject<ClearGlassPaneBlock> searedGlassPane, scorchedGlassPane;
  public static final ItemObject<SearedTintedGlassBlock> searedTintedGlass, scorchedTintedGlass;
  static {
    Supplier<Properties> seared = () -> searedNonSolidProps(SoundType.GLASS);
    searedGlass = BLOCKS.register("seared_glass", () -> new SearedGlassBlock(seared.get()), TOOLTIP_BLOCK_ITEM);
    searedGlassPane = BLOCKS.register("seared_glass_pane", () -> new ClearGlassPaneBlock(seared.get()), TOOLTIP_BLOCK_ITEM);
    searedTintedGlass = BLOCKS.register("seared_tinted_glass", () -> new SearedTintedGlassBlock(seared.get()), TOOLTIP_BLOCK_ITEM);

    Supplier<Properties> scorched = () -> scorchedNonSolidProps(SoundType.GLASS);
    scorchedGlass = BLOCKS.register("scorched_glass", () -> new SearedGlassBlock(scorched.get()), TOOLTIP_BLOCK_ITEM);
    scorchedTintedGlass = BLOCKS.register("scorched_tinted_glass", () -> new SearedTintedGlassBlock(scorched.get()), TOOLTIP_BLOCK_ITEM);
    scorchedGlassPane = BLOCKS.register("scorched_glass_pane", () -> new ClearGlassPaneBlock(scorched.get()), TOOLTIP_BLOCK_ITEM);
  }
  // soul glass
  public static final ItemObject<SearedSoulGlassBlock> searedSoulGlass, scorchedSoulGlass;
  public static final ItemObject<SoulGlassPaneBlock> searedSoulGlassPane, scorchedSoulGlassPane;
  static {
    Supplier<Properties> seared = () -> searedNonSolidProps(SoundType.GLASS).noCollision().speedFactor(0.1f).isViewBlocking((state, getter, pos) -> true);
    searedSoulGlass = BLOCKS.register("seared_soul_glass", () -> new SearedSoulGlassBlock(seared.get()), TOOLTIP_BLOCK_ITEM);
    searedSoulGlassPane = BLOCKS.register("seared_soul_glass_pane", () -> new SoulGlassPaneBlock(seared.get()), TOOLTIP_BLOCK_ITEM);

    Supplier<Properties> scorched = () -> scorchedNonSolidProps(SoundType.GLASS).noCollision().speedFactor(0.1f).isViewBlocking((state, getter, pos) -> true);
    scorchedSoulGlass = BLOCKS.register("scorched_soul_glass", () -> new SearedSoulGlassBlock(scorched.get()), TOOLTIP_BLOCK_ITEM);
    scorchedSoulGlassPane = BLOCKS.register("scorched_soul_glass_pane", () -> new SoulGlassPaneBlock(scorched.get()), TOOLTIP_BLOCK_ITEM);
  }

  // peripherals
  public static final ItemObject<Block> searedDrain, searedDuct, searedChute;
  public static final ItemObject<Block> scorchedDrain, scorchedDuct, scorchedChute;
  static {
    Supplier<Properties> seared = () -> searedSolidProps(2);
    searedDrain = BLOCKS.register("seared_drain", () -> new SearedDrainBlock(seared.get()), TOOLTIP_BLOCK_ITEM);
    searedDuct = BLOCKS.register("seared_duct", () -> new SearedDuctBlock(seared.get()), TOOLTIP_BLOCK_ITEM);
    searedChute = BLOCKS.register("seared_chute", () -> new RetexturedOrientableSmelteryBlock(seared.get(), ChuteBlockEntity::new), TOOLTIP_BLOCK_ITEM);

    Supplier<Properties> scorched = () -> scorchedSolidProps(3);
    scorchedDrain = BLOCKS.register("scorched_drain", () -> new SearedDrainBlock(scorched.get()), TOOLTIP_BLOCK_ITEM);
    scorchedDuct = BLOCKS.register("scorched_duct", () -> new SearedDuctBlock(scorched.get()), TOOLTIP_BLOCK_ITEM);
    scorchedChute = BLOCKS.register("scorched_chute", () -> new RetexturedOrientableSmelteryBlock(scorched.get(), ChuteBlockEntity::new), TOOLTIP_BLOCK_ITEM);
  }

  // non-solid blocks
  public static final ItemObject<SearedLadderBlock> searedLadder, scorchedLadder;
  public static final ItemObject<FaucetBlock> searedFaucet, scorchedFaucet;
  public static final ItemObject<ChannelBlock> searedChannel, scorchedChannel;
  public static final ItemObject<CastingBasinBlock> searedBasin, scorchedBasin;
  public static final ItemObject<CastingTableBlock> searedTable, scorchedTable;
  public static final ItemObject<ProxyTankBlock> scorchedProxyTank;
  static {
    Supplier<Properties> seared = () -> searedNonSolidProps(SoundType.METAL);
    searedLadder = BLOCKS.register("seared_ladder", () -> new SearedLadderBlock(seared.get()), TOOLTIP_BLOCK_ITEM);
    searedFaucet = BLOCKS.register("seared_faucet", () -> new FaucetBlock(seared.get()), TOOLTIP_BLOCK_ITEM);
    searedChannel = BLOCKS.register("seared_channel", () -> new ChannelBlock(seared.get()), TOOLTIP_BLOCK_ITEM);
    searedBasin = BLOCKS.register("seared_basin", () -> new CastingBasinBlock(seared.get(), false), TOOLTIP_BLOCK_ITEM);
    searedTable = BLOCKS.register("seared_table", () -> new CastingTableBlock(seared.get(), false), TOOLTIP_BLOCK_ITEM);

    Supplier<Properties> scorched = () -> scorchedNonSolidProps(SoundType.BASALT);
    scorchedLadder = BLOCKS.register("scorched_ladder", () -> new SearedLadderBlock(scorched.get()), TOOLTIP_BLOCK_ITEM);
    scorchedFaucet = BLOCKS.register("scorched_faucet", () -> new FaucetBlock(scorched.get()), TOOLTIP_BLOCK_ITEM);
    scorchedChannel = BLOCKS.register("scorched_channel", () -> new ChannelBlock(scorched.get()), TOOLTIP_BLOCK_ITEM);
    scorchedBasin = BLOCKS.register("scorched_basin", () -> new CastingBasinBlock(scorched.get(), true), TOOLTIP_BLOCK_ITEM);
    scorchedTable = BLOCKS.register("scorched_table", () -> new CastingTableBlock(scorched.get(), true), TOOLTIP_BLOCK_ITEM);
    scorchedProxyTank = BLOCKS.register("scorched_proxy_tank", () -> new ProxyTankBlock(scorched.get()), TOOLTIP_BLOCK_ITEM);
  }

  // tank
  public static final EnumObject<TankType,SearedTankBlock> searedTank, scorchedTank;
  public static final ItemObject<CastingTankBlock> searedCastingTank;
  public static final ItemObject<FluidCannonBlock> searedFluidCannon, scorchedFluidCannon, endFluidCannon;
  public static final ItemObject<SearedLanternBlock> searedLantern, scorchedLantern;
  static {
    Function<Block, BlockItem> tankItem = b -> new TankItem(b, itemProperties(ITEM_PROPS), true);
    Function<Block, BlockItem> lanternItem = b -> new TankItem(b, itemProperties(ITEM_PROPS), false);
    // seared
    Supplier<Properties> seared = () -> searedNonSolidProps(SoundType.METAL).lightLevel(SearedTankBlock.LIGHT_GETTER);
    searedTank = BLOCKS.registerEnum("seared", SearedTankBlock.TankType.values(), type -> new SearedTankBlock(seared.get(), type.getCapacity(), PushReaction.DESTROY), tankItem);
    searedCastingTank = BLOCKS.register("seared_casting_tank", () -> new CastingTankBlock(seared.get()), b -> new TankItem(b, itemProperties(ITEM_PROPS), true));
    searedFluidCannon = BLOCKS.register("seared_fluid_cannon", () -> new FluidCannonBlock(seared.get(), FluidType.BUCKET_VOLUME * 2, 1.0f, 1.1f, 6.0f), tankItem);
    searedLantern = BLOCKS.register("seared_lantern", () -> new SearedLanternBlock(searedNonSolidProps(SoundType.LANTERN).lightLevel(SearedTankBlock.LIGHT_GETTER), FluidValues.LANTERN_CAPACITY), lanternItem);
    // scorched
    Supplier<Properties> scorched = () -> scorchedNonSolidProps(SoundType.BASALT).lightLevel(SearedTankBlock.LIGHT_GETTER);
    scorchedTank = BLOCKS.registerEnum("scorched", SearedTankBlock.TankType.values(), type -> new SearedTankBlock(scorched.get(), type.getCapacity(), PushReaction.DESTROY), tankItem);
    scorchedFluidCannon = BLOCKS.register("scorched_fluid_cannon", () -> new FluidCannonBlock(scorched.get(), FluidType.BUCKET_VOLUME * 2, 2.0f, 1.5f, 7.0f), tankItem);
    scorchedLantern = BLOCKS.register("scorched_lantern", () -> new SearedLanternBlock(scorchedNonSolidProps(SoundType.LANTERN).lightLevel(SearedTankBlock.LIGHT_GETTER), FluidValues.LANTERN_CAPACITY), lanternItem);
    // end
    endFluidCannon = BLOCKS.register("end_fluid_cannon", () -> new KnightMetalFluidCannonBlock(seared.get(), FluidType.BUCKET_VOLUME * 4, 1.5f, 3.0f, 4.0f), tankItem);
  }

  // utility
  public static final ItemObject<GaugeBlock> copperGauge, obsidianGauge;
  static {
    Supplier<Properties> gaugeProperties = () -> blockProperties().mapColor(MapColor.NONE).pushReaction(PushReaction.DESTROY).noCollision().strength(0.5F).noOcclusion().requiresCorrectToolForDrops();
    copperGauge = BLOCKS.register("copper_gauge", () -> new GaugeBlock(gaugeProperties.get()), TOOLTIP_BLOCK_ITEM);
    obsidianGauge = BLOCKS.register("obsidian_gauge", () -> new RenderingGaugeBlock(gaugeProperties.get()), TOOLTIP_BLOCK_ITEM);
  }

  // controllers
  public static final ItemObject<SmelteryControllerBlock> smelteryController;
  public static final ItemObject<FoundryControllerBlock> foundryController;
  // tiny
  public static final ItemObject<MelterBlock> searedMelter;
  public static final ItemObject<HeaterBlock> searedHeater;
  public static final ItemObject<AlloyerBlock> scorchedAlloyer;
  static {
    Supplier<Properties> seared = () -> builder(MapColor.COLOR_GRAY, SoundType.METAL).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(8.0F, 28F).lightLevel(s -> s.getValue(ControllerBlock.ACTIVE) ? 13 : 0);
    Supplier<Properties> scorched = () -> builder(MapColor.TERRACOTTA_BROWN, SoundType.BASALT).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(9.0F, 35f).lightLevel(s -> s.getValue(ControllerBlock.ACTIVE) ? 13 : 0);
    smelteryController = BLOCKS.register("smeltery_controller", () -> new SmelteryControllerBlock(seared.get()),  TOOLTIP_BLOCK_ITEM);
    foundryController = BLOCKS.register("foundry_controller", () -> new FoundryControllerBlock(scorched.get()),  TOOLTIP_BLOCK_ITEM);
    // tiny
    searedMelter = BLOCKS.register("seared_melter", () -> new MelterBlock(seared.get().noOcclusion()), TOOLTIP_BLOCK_ITEM);
    searedHeater = BLOCKS.register("seared_heater", () -> new HeaterBlock(seared.get()), TOOLTIP_BLOCK_ITEM);
    scorchedAlloyer = BLOCKS.register("scorched_alloyer", () -> new AlloyerBlock(scorched.get().noOcclusion()), TOOLTIP_BLOCK_ITEM);
  }

  /*
   * Tile entities
   */
  // smeltery
  public static final DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<SmelteryComponentBlockEntity>> smelteryComponent = BLOCK_ENTITIES.register("smeltery_component", SmelteryComponentBlockEntity::new, set -> {
    // seared
    set.addAll(searedStone.values());
    set.addAll(searedCobble.values());
    set.addAll(searedBricks.values());
    set.addAll(searedPaver.values());
    set.add(searedCrackedBricks.get(), searedFancyBricks.get(), searedTriangleBricks.get(), searedLadder.get(), searedLamp.get(), searedGlass.get(), searedSoulGlass.get(), searedTintedGlass.get());
    // scorched
    set.add(scorchedStone.get(), polishedScorchedStone.get(), chiseledScorchedBricks.get(), scorchedLadder.get(), scorchedLamp.get(), scorchedGlass.get(), scorchedSoulGlass.get(), scorchedTintedGlass.get());
    set.addAll(scorchedBricks.values());
    set.addAll(scorchedRoad.values());
  });
  public static final DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<SmelteryFluidIO>> drain = BLOCK_ENTITIES.register("drain", DrainBlockEntity::new, set -> set.add(searedDrain.get(), scorchedDrain.get()));
  public static final DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<ChuteBlockEntity>> chute = BLOCK_ENTITIES.register("chute", ChuteBlockEntity::new, set -> set.add(searedChute.get(), scorchedChute.get()));
  public static final DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<DuctBlockEntity>> duct = BLOCK_ENTITIES.register("duct", DuctBlockEntity::new, set -> set.add(searedDuct.get(), scorchedDuct.get()));
  public static final DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<TankBlockEntity>> tank = BLOCK_ENTITIES.register("tank", TankBlockEntity::new, set -> {
    set.addAll(searedTank.values());
    set.addAll(scorchedTank.values());
  });
  public static final DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<FluidCannonBlockEntity>> fluidCannon = BLOCK_ENTITIES.register("fluid_cannon", FluidCannonBlockEntity::new, set -> set.add(searedFluidCannon.get(), scorchedFluidCannon.get(), endFluidCannon.get()));
  public static final DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<LanternBlockEntity>> lantern = BLOCK_ENTITIES.register("lantern", LanternBlockEntity::new, set -> set.add(searedLantern.get(), scorchedLantern.get()));
  // controller
  public static final DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<MelterBlockEntity>> melter = BLOCK_ENTITIES.register("melter", MelterBlockEntity::new, searedMelter);
  public static final DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<SmelteryBlockEntity>> smeltery = BLOCK_ENTITIES.register("smeltery", SmelteryBlockEntity::new, smelteryController);
  public static final DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<FoundryBlockEntity>> foundry = BLOCK_ENTITIES.register("foundry", FoundryBlockEntity::new, foundryController);
  public static final DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<HeaterBlockEntity>> heater = BLOCK_ENTITIES.register("heater", HeaterBlockEntity::new, searedHeater);
  public static final DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<AlloyerBlockEntity>> alloyer = BLOCK_ENTITIES.register("alloyer", AlloyerBlockEntity::new, scorchedAlloyer);
  // fluid transfer
  public static final DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<FaucetBlockEntity>> faucet = BLOCK_ENTITIES.register("faucet", FaucetBlockEntity::new, set -> set.add(searedFaucet.get(), scorchedFaucet.get()));
  public static final DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<ChannelBlockEntity>> channel = BLOCK_ENTITIES.register("channel", ChannelBlockEntity::new, set -> set.add(searedChannel.get(), scorchedChannel.get()));
  public static final DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<GaugeBlockEntity>> gauge = BLOCK_ENTITIES.register("gauge", GaugeBlockEntity::new, obsidianGauge);
  // casting
  public static final DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<CastingBlockEntity>> basin = BLOCK_ENTITIES.register("basin", CastingBlockEntity.Basin::new, set -> set.add(searedBasin.get(), scorchedBasin.get()));
  public static final DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<CastingBlockEntity>> table = BLOCK_ENTITIES.register("table", CastingBlockEntity.Table::new, set -> set.add(searedTable.get(), scorchedTable.get()));
  public static final DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<ProxyTankBlockEntity>> proxyTank = BLOCK_ENTITIES.register("proxy_tank", ProxyTankBlockEntity::new, scorchedProxyTank);
  // casting tank
  public static final DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<CastingTankBlockEntity>> castingTank = BLOCK_ENTITIES.register("casting_tank", CastingTankBlockEntity::new, set -> set.add(searedCastingTank.get()));

  /*
   * Items
   */
  public static final ItemObject<Item> searedBrick = ITEMS.register("seared_brick", itemProperties(ITEM_PROPS));
  public static final ItemObject<Item> scorchedBrick = ITEMS.register("scorched_brick", itemProperties(ITEM_PROPS));
  public static final ItemObject<Item> copperCan = ITEMS.register("copper_can", () -> new CopperCanItem(itemProperties().stacksTo(16)));

  // casts
  // basic
  public static final ItemObject<Item> blankSandCast  = ITEMS.register("blank_sand_cast", itemProperties(ITEM_PROPS));
  public static final ItemObject<Item> blankRedSandCast  = ITEMS.register("blank_red_sand_cast", itemProperties(ITEM_PROPS));
  public static final CastItemObject ingotCast  = ITEMS.registerCast("ingot", itemProperties(ITEM_PROPS));
  public static final CastItemObject nuggetCast = ITEMS.registerCast("nugget", itemProperties(ITEM_PROPS));
  public static final CastItemObject gemCast    = ITEMS.registerCast("gem", itemProperties(ITEM_PROPS));
  public static final CastItemObject rodCast    = ITEMS.registerCast("rod", itemProperties(ITEM_PROPS));
  public static final CastItemObject repairKitCast = ITEMS.registerCast(TinkerToolParts.repairKit, itemProperties(ITEM_PROPS));
  // compatability
  public static final CastItemObject plateCast  = ITEMS.registerCast("plate", itemProperties(ITEM_PROPS));
  public static final CastItemObject gearCast   = ITEMS.registerCast("gear", itemProperties(ITEM_PROPS));
  public static final CastItemObject coinCast   = ITEMS.registerCast("coin", itemProperties(ITEM_PROPS));
  public static final CastItemObject wireCast   = ITEMS.registerCast("wire", itemProperties(ITEM_PROPS));
  // small tool heads
  public static final CastItemObject pickHeadCast = ITEMS.registerCast(TinkerToolParts.pickHead, itemProperties(ITEM_PROPS));
  public static final CastItemObject smallAxeHeadCast = ITEMS.registerCast(TinkerToolParts.smallAxeHead, itemProperties(ITEM_PROPS));
  public static final CastItemObject smallBladeCast = ITEMS.registerCast(TinkerToolParts.smallBlade, itemProperties(ITEM_PROPS));
  public static final CastItemObject adzeHeadCast = ITEMS.registerCast(TinkerToolParts.adzeHead, itemProperties(ITEM_PROPS));
  // large tool heads
  public static final CastItemObject hammerHeadCast   = ITEMS.registerCast(TinkerToolParts.hammerHead, itemProperties(ITEM_PROPS));
  public static final CastItemObject broadBladeCast   = ITEMS.registerCast(TinkerToolParts.broadBlade, itemProperties(ITEM_PROPS));
  public static final CastItemObject broadAxeHeadCast = ITEMS.registerCast(TinkerToolParts.broadAxeHead, itemProperties(ITEM_PROPS));
  public static final CastItemObject largePlateCast  = ITEMS.registerCast(TinkerToolParts.largePlate, itemProperties(ITEM_PROPS));
  // bindings
  public static final CastItemObject toolBindingCast = ITEMS.registerCast(TinkerToolParts.toolBinding, itemProperties(ITEM_PROPS));
  public static final CastItemObject toughBindingCast = ITEMS.registerCast(TinkerToolParts.toughBinding, itemProperties(ITEM_PROPS));
  // tool rods
  public static final CastItemObject toolHandleCast  = ITEMS.registerCast(TinkerToolParts.toolHandle, itemProperties(ITEM_PROPS));
  public static final CastItemObject toughHandleCast = ITEMS.registerCast(TinkerToolParts.toughHandle, itemProperties(ITEM_PROPS));
  // bow
  public static final CastItemObject bowLimbCast = ITEMS.registerCast(TinkerToolParts.bowLimb, itemProperties(ITEM_PROPS));
  public static final CastItemObject bowGripCast = ITEMS.registerCast(TinkerToolParts.bowGrip, itemProperties(ITEM_PROPS));
  public static final ItemObject<Item> arrowCast = ITEMS.register("arrow_cast", TOOLTIP_ITEM);
  // armor
  public static final CastItemObject helmetPlatingCast = ITEMS.registerCast("helmet_plating", () -> new PartCastItem(itemProperties(ITEM_PROPS), () -> TinkerToolParts.plating.get(ArmorSlotType.HELMET)));
  public static final CastItemObject chestplatePlatingCast = ITEMS.registerCast("chestplate_plating", () -> new PartCastItem(itemProperties(ITEM_PROPS), () -> TinkerToolParts.plating.get(ArmorSlotType.CHESTPLATE)));
  public static final CastItemObject leggingsPlatingCast = ITEMS.registerCast("leggings_plating", () -> new PartCastItem(itemProperties(ITEM_PROPS), () -> TinkerToolParts.plating.get(ArmorSlotType.LEGGINGS)));
  public static final CastItemObject bootsPlatingCast = ITEMS.registerCast("boots_plating", () -> new PartCastItem(itemProperties(ITEM_PROPS), () -> TinkerToolParts.plating.get(ArmorSlotType.BOOTS)));
  public static final CastItemObject mailleCast = ITEMS.registerCast(TinkerToolParts.maille, itemProperties(ITEM_PROPS));
  // dummy cast creation items
  public static final EnumObject<ArmorSlotType,DummyMaterialItem> dummyPlating = ITEMS.registerEnum(slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial.ARMOR_TYPES, "plating_dummy", type -> new DummyMaterialItem(itemProperties(ITEM_PROPS)));


  /*
   * Recipe
   */
  // casting
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<ItemCastingRecipe>> basinRecipeSerializer = RECIPE_SERIALIZERS.register("casting_basin", () -> LoadableRecipeSerializer.of(ItemCastingRecipe.LOADER, TinkerRecipeTypes.CASTING_BASIN));
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<ItemCastingRecipe>> tableRecipeSerializer = RECIPE_SERIALIZERS.register("casting_table", () -> LoadableRecipeSerializer.of(ItemCastingRecipe.LOADER, TinkerRecipeTypes.CASTING_TABLE));
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<ContainerFillingRecipe>> basinFillingRecipeSerializer = RECIPE_SERIALIZERS.register("basin_filling", () -> LoadableRecipeSerializer.of(ContainerFillingRecipe.LOADER, TinkerRecipeTypes.CASTING_BASIN));
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<ContainerFillingRecipe>> tableFillingRecipeSerializer = RECIPE_SERIALIZERS.register("table_filling", () -> LoadableRecipeSerializer.of(ContainerFillingRecipe.LOADER, TinkerRecipeTypes.CASTING_TABLE));
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<CastDuplicationRecipe>> basinDuplicationRecipeSerializer = RECIPE_SERIALIZERS.register("basin_duplication", () -> LoadableRecipeSerializer.of(CastDuplicationRecipe.LOADER, TinkerRecipeTypes.CASTING_BASIN));
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<CastDuplicationRecipe>> tableDuplicationRecipeSerializer = RECIPE_SERIALIZERS.register("table_duplication", () -> LoadableRecipeSerializer.of(CastDuplicationRecipe.LOADER, TinkerRecipeTypes.CASTING_TABLE));
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<PotionCastingRecipe>> basinPotionRecipeSerializer = RECIPE_SERIALIZERS.register("casting_basin_potion", () -> LoadableRecipeSerializer.of(PotionCastingRecipe.LOADER, TinkerRecipeTypes.CASTING_BASIN));
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<PotionCastingRecipe>> tablePotionRecipeSerializer = RECIPE_SERIALIZERS.register("casting_table_potion", () -> LoadableRecipeSerializer.of(PotionCastingRecipe.LOADER, TinkerRecipeTypes.CASTING_TABLE));
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<TippingCastingRecipe>> basinTippingRecipeSerializer = RECIPE_SERIALIZERS.register("casting_basin_tipping", () -> LoadableRecipeSerializer.of(TippingCastingRecipe.LOADER, TinkerRecipeTypes.CASTING_BASIN));
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<TippingCastingRecipe>> tableTippingRecipeSerializer = RECIPE_SERIALIZERS.register("casting_table_tipping", () -> LoadableRecipeSerializer.of(TippingCastingRecipe.LOADER, TinkerRecipeTypes.CASTING_TABLE));
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<TipClearingCastingRecipe>> basinTipClearingRecipeSerializer = RECIPE_SERIALIZERS.register("casting_basin_tipped_clearing", () -> LoadableRecipeSerializer.of(TipClearingCastingRecipe.LOADER, TinkerRecipeTypes.CASTING_BASIN));
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<TipClearingCastingRecipe>> tableTipClearingRecipeSerializer = RECIPE_SERIALIZERS.register("casting_table_tipped_clearing", () -> LoadableRecipeSerializer.of(TipClearingCastingRecipe.LOADER, TinkerRecipeTypes.CASTING_TABLE));
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<RetexturedCastingRecipe>> retexturedBasinRecipeSerializer = RECIPE_SERIALIZERS.register("retextured_casting_basin", () -> LoadableRecipeSerializer.of(RetexturedCastingRecipe.LOADER, TinkerRecipeTypes.CASTING_BASIN));
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<RetexturedCastingRecipe>> retexturedTableRecipeSerializer = RECIPE_SERIALIZERS.register("retextured_casting_table", () -> LoadableRecipeSerializer.of(RetexturedCastingRecipe.LOADER, TinkerRecipeTypes.CASTING_TABLE));
  // material casting
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<MaterialCastingRecipe>> basinMaterialSerializer = RECIPE_SERIALIZERS.register("basin_casting_material", () -> LoadableRecipeSerializer.of(MaterialCastingRecipe.LOADER, TinkerRecipeTypes.CASTING_BASIN));
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<MaterialCastingRecipe>> tableMaterialSerializer = RECIPE_SERIALIZERS.register("table_casting_material", () -> LoadableRecipeSerializer.of(MaterialCastingRecipe.LOADER, TinkerRecipeTypes.CASTING_TABLE));
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<CompositeCastingRecipe>> basinCompositeSerializer = RECIPE_SERIALIZERS.register("basin_casting_composite", () -> LoadableRecipeSerializer.of(CompositeCastingRecipe.LOADER, TinkerRecipeTypes.CASTING_BASIN));
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<CompositeCastingRecipe>> tableCompositeSerializer = RECIPE_SERIALIZERS.register("table_casting_composite", () -> LoadableRecipeSerializer.of(CompositeCastingRecipe.LOADER, TinkerRecipeTypes.CASTING_TABLE));
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<ToolCastingRecipe>> basinToolSerializer = RECIPE_SERIALIZERS.register("basin_tool_casting", () -> LoadableRecipeSerializer.of(ToolCastingRecipe.LOADER, TinkerRecipeTypes.CASTING_BASIN));
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<ToolCastingRecipe>> tableToolSerializer = RECIPE_SERIALIZERS.register("table_tool_casting", () -> LoadableRecipeSerializer.of(ToolCastingRecipe.LOADER, TinkerRecipeTypes.CASTING_TABLE));
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<MaterialFluidRecipe>> materialFluidRecipe = RECIPE_SERIALIZERS.register("material_fluid", () -> LoadableRecipeSerializer.of(MaterialFluidRecipe.LOADER));
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<PartSwapCastingRecipe>> basinPartSwappingSerializer = RECIPE_SERIALIZERS.register("basin_casting_part_swapping", () -> LoadableRecipeSerializer.of(PartSwapCastingRecipe.LOADER, TinkerRecipeTypes.CASTING_BASIN));
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<PartSwapCastingRecipe>> tablePartSwappingSerializer = RECIPE_SERIALIZERS.register("table_casting_part_swapping", () -> LoadableRecipeSerializer.of(PartSwapCastingRecipe.LOADER, TinkerRecipeTypes.CASTING_TABLE));
  // molding
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<MoldingRecipe>> moldingBasinSerializer = RECIPE_SERIALIZERS.register("molding_basin", () -> LoadableRecipeSerializer.of(MoldingRecipe.LOADER, TinkerRecipeTypes.MOLDING_BASIN));
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<MoldingRecipe>> moldingTableSerializer = RECIPE_SERIALIZERS.register("molding_table", () -> LoadableRecipeSerializer.of(MoldingRecipe.LOADER, TinkerRecipeTypes.MOLDING_TABLE));
  // melting
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<MeltingRecipe>> meltingSerializer = RECIPE_SERIALIZERS.register("melting", () -> LoadableRecipeSerializer.of(MeltingRecipe.LOADER));
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<OreMeltingRecipe>> oreMeltingSerializer = RECIPE_SERIALIZERS.register("ore_melting", () -> LoadableRecipeSerializer.of(OreMeltingRecipe.LOADER));
  // TODO 1.21: correct misspelled name
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<DamageableMeltingRecipe>> damagableMeltingSerializer = RECIPE_SERIALIZERS.register("damagable_melting", () -> LoadableRecipeSerializer.of(DamageableMeltingRecipe.LOADER));
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<MaterialMeltingRecipe>> materialMeltingSerializer = RECIPE_SERIALIZERS.register("material_melting", () -> LoadableRecipeSerializer.of(MaterialMeltingRecipe.LOADER));
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<MeltingFuel>> fuelSerializer = RECIPE_SERIALIZERS.register("melting_fuel", () -> LoadableRecipeSerializer.of(MeltingFuel.LOADER));
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<EntityMeltingRecipe>> entityMeltingSerializer = RECIPE_SERIALIZERS.register("entity_melting", () -> LoadableRecipeSerializer.of(EntityMeltingRecipe.LOADER));
  // alloying
  public static final DeferredHolder<RecipeSerializer<?>, ? extends RecipeSerializer<AlloyRecipe>> alloyingSerializer = RECIPE_SERIALIZERS.register("alloy", () -> LoadableRecipeSerializer.of(AlloyRecipe.LOADER));

  /*
   * Inventory
   */
  public static final DeferredHolder<MenuType<?>, ? extends MenuType<MelterContainerMenu>> melterContainer = MENUS.register("melter", MelterContainerMenu::new);
  public static final DeferredHolder<MenuType<?>, ? extends MenuType<HeatingStructureContainerMenu>> smelteryContainer = MENUS.register("smeltery", HeatingStructureContainerMenu::new);
  public static final DeferredHolder<MenuType<?>, ? extends MenuType<SingleItemContainerMenu>> singleItemContainer = MENUS.register("single_item", SingleItemContainerMenu::new);
  public static final DeferredHolder<MenuType<?>, ? extends MenuType<AlloyerContainerMenu>> alloyerContainer = MENUS.register("alloyer", AlloyerContainerMenu::new);

  @SubscribeEvent
  void commonSetup(FMLCommonSetupEvent event) {
    event.enqueueWork(() -> {
      Consumer<Block> dispenserBehavior = block -> DispenserBlock.registerBehavior(block.asItem(), PlaceBlockDispenserBehavior.INSTANCE);
      searedTank.forEach(dispenserBehavior);
      scorchedTank.forEach(dispenserBehavior);
    });
  }

  @SuppressWarnings("removal")
  @SubscribeEvent
  void registerSerializers(RegisterEvent event) {
    if (event.getRegistryKey() == Registries.RECIPE_SERIALIZER) {
      FluidContainerTransferManager.TRANSFER_LOADERS.registerDeserializer(EmptyPotionTransfer.ID, EmptyPotionTransfer.DESERIALIZER);
    }
  }

  /** Adds all relevant items to the creative tab */
  private static void addTabItems(ItemDisplayParameters itemDisplayParameters, Output output) {
    // crafting materials
    output.accept(grout);
    output.accept(searedBrick);
    output.accept(netherGrout);
    output.accept(scorchedBrick);
    output.accept(copperCan);

    // controllers
    output.accept(searedMelter);
    output.accept(searedHeater);
    output.accept(scorchedAlloyer);
    output.accept(smelteryController);
    output.accept(foundryController);

    // IO blocks
    output.accept(searedDrain);
    output.accept(searedDuct);
    output.accept(searedChute);
    output.accept(scorchedDrain);
    output.accept(scorchedDuct);
    output.accept(scorchedChute);

    // tanks
    output.accept(copperGauge);
    output.accept(obsidianGauge);
    accept(output, searedTank);
    // toss in some pre filled fuel tanks
    output.accept(TankItem.fillTank(searedTank, TankType.FUEL_TANK, Fluids.LAVA));
    output.accept(TankItem.fillTank(searedTank, TankType.FUEL_TANK, TinkerFluids.blazingBlood.get()));
    output.accept(searedLantern);
    accept(output, scorchedTank);
    output.accept(TankItem.fillTank(scorchedTank, TankType.FUEL_TANK, Fluids.LAVA));
    output.accept(TankItem.fillTank(scorchedTank, TankType.FUEL_TANK, TinkerFluids.blazingBlood.get()));
    output.accept(scorchedLantern);

    // fluid transfer
    output.accept(searedFaucet);
    output.accept(scorchedFaucet);
    output.accept(searedChannel);
    output.accept(scorchedChannel);
    // casting
    output.accept(searedTable);
    output.accept(scorchedTable);
    output.accept(TinkerCommons.goldBars, TabVisibility.PARENT_TAB_ONLY);
    output.accept(searedBasin);
    output.accept(scorchedBasin);
    output.accept(TinkerCommons.goldPlatform, TabVisibility.PARENT_TAB_ONLY);
    output.accept(searedCastingTank);
    output.accept(scorchedProxyTank);

    // cannons
    output.accept(searedFluidCannon);
    output.accept(scorchedFluidCannon);
    output.accept(endFluidCannon);

    // seared blocks
    accept(output, searedBricks);
    accept(output, searedStone);
    output.accept(searedCrackedBricks);
    output.accept(searedFancyBricks);
    output.accept(searedTriangleBricks);
    accept(output, searedCobble);
    accept(output, searedPaver);
    output.accept(searedLamp);
    output.accept(searedLadder);
    output.accept(searedGlass);
    output.accept(searedTintedGlass);
    output.accept(searedSoulGlass);
    output.accept(searedGlassPane);
    output.accept(searedSoulGlassPane);

    // scorched blocks
    accept(output, scorchedBricks);
    output.accept(chiseledScorchedBricks);
    output.accept(scorchedStone);
    output.accept(polishedScorchedStone);
    accept(output, scorchedRoad);
    output.accept(scorchedLamp);
    output.accept(scorchedLadder);
    output.accept(scorchedGlass);
    output.accept(scorchedTintedGlass);
    output.accept(scorchedSoulGlass);
    output.accept(scorchedGlassPane);
    output.accept(scorchedSoulGlassPane);

    // casts
    addCasts(output, CastItemObject::get);
    output.accept(blankSandCast);
    addCasts(output, CastItemObject::getSand);
    output.accept(blankRedSandCast);
    addCasts(output, CastItemObject::getRedSand);
    // dummy parts are in tool parts creative tab

    // buckets and other fluid items
    TinkerFluids.addTabItems(itemDisplayParameters, output);
  }

  /** Adds retextured controller, drain, duct, and chute variants to the decoration tab. */
  public static void addTableVariants(ItemDisplayParameters itemDisplayParameters, Output output) {
    // additional texture variants of controllers, drains, and ducts
    Predicate<ItemStack> variant = stack -> {
      output.accept(stack);
      return false;
    };
    RetexturedHelper.addTagVariants(variant, smelteryController, TinkerTags.Items.SEARED_BLOCKS);
    RetexturedHelper.addTagVariants(variant, searedDrain, TinkerTags.Items.SEARED_BLOCKS);
    RetexturedHelper.addTagVariants(variant, searedDuct, TinkerTags.Items.SEARED_BLOCKS);
    RetexturedHelper.addTagVariants(variant, searedChute, TinkerTags.Items.SEARED_BLOCKS);
    RetexturedHelper.addTagVariants(variant, foundryController, TinkerTags.Items.SCORCHED_BLOCKS);
    RetexturedHelper.addTagVariants(variant, scorchedDrain, TinkerTags.Items.SCORCHED_BLOCKS);
    RetexturedHelper.addTagVariants(variant, scorchedDuct, TinkerTags.Items.SCORCHED_BLOCKS);
    RetexturedHelper.addTagVariants(variant, scorchedChute, TinkerTags.Items.SCORCHED_BLOCKS);
  }

  /** Adds adds all casts of the given type to the tab */
  private static void addCasts(CreativeModeTab.Output output, Function<CastItemObject,ItemLike> getter) {
    // common casts
    accept(output, getter, ingotCast);
    accept(output, getter, nuggetCast);
    accept(output, getter, gemCast);
    accept(output, getter, rodCast);
    accept(output, getter, repairKitCast);
    // compat casts
    acceptIfTag(output, getter, plateCast);
    acceptIfTag(output, getter, gearCast);
    acceptIfTag(output, getter, coinCast);
    acceptIfTag(output, getter, wireCast);
    // small heads
    accept(output, getter, pickHeadCast);
    accept(output, getter, smallAxeHeadCast);
    accept(output, getter, smallBladeCast);
    accept(output, getter, adzeHeadCast);
    // large heads
    accept(output, getter, hammerHeadCast);
    accept(output, getter, broadAxeHeadCast);
    accept(output, getter, broadBladeCast);
    accept(output, getter, largePlateCast);
    // binding and rods
    accept(output, getter, toolHandleCast);
    accept(output, getter, toolBindingCast);
    accept(output, getter, toughHandleCast);
    accept(output, getter, toughBindingCast);
    // ranged
    accept(output, getter, bowLimbCast);
    accept(output, getter, bowGripCast);
    output.accept(arrowCast);
    // no binding cast
    // armor
    accept(output, getter, helmetPlatingCast);
    accept(output, getter, chestplatePlatingCast);
    accept(output, getter, leggingsPlatingCast);
    accept(output, getter, bootsPlatingCast);
    accept(output, getter, mailleCast);
  }

  /** Adds a cast to the tab */
  private static void accept(CreativeModeTab.Output output, Function<CastItemObject,ItemLike> getter, CastItemObject cast) {
    output.accept(getter.apply(cast));
  }

  /** Adds a cast to the tab */
  private static void acceptIfTag(CreativeModeTab.Output output, Function<CastItemObject,ItemLike> getter, CastItemObject cast) {
    acceptIfTag(output, getter.apply(cast), ItemTags.create(commonResource(cast.getName().getPath() + "s")));
  }


  // properties

  /** Properties for smeltery or foundry blocks. */
  private static Properties structureProps(MapColor color, SoundType sound) {
    return builder(color, sound).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().isValidSpawn(SearedBlock.VALID_SPAWN);
  }

  /** Properties for transparent smeltery or foundry blocks, such as glass. */
  private static Properties structureNonSolid(MapColor color, SoundType sound) {
    return structureProps(color, sound).isValidSpawn((state, level, pos, entity) -> false).isRedstoneConductor(NEVER).isSuffocating(NEVER).isViewBlocking(NEVER).noOcclusion().forceSolidOn();
  }

  /** Properties for an opaque seared block, such as bricks. */
  private static Properties searedSolidProps(int factor) {
    return structureProps(MapColor.COLOR_GRAY, SoundType.METAL).strength(3.0F * factor, 9.0F * factor);
  }

  /** Properties for a transparent seared block, such as glass. */
  private static Properties searedNonSolidProps(SoundType sound) {
    return structureNonSolid(MapColor.COLOR_GRAY, sound).strength(3.0F, 9.0F);
  }

  /** Properties for an opaque seared block, such as bricks. */
  private static Properties scorchedSolidProps(int factor) {
    return structureProps(MapColor.TERRACOTTA_BROWN, SoundType.BASALT).strength(2.5F * factor, 8.0F * factor);
  }

  /** Properties for a transparent seared block, such as glass. */
  private static Properties scorchedNonSolidProps(SoundType sound) {
    return structureNonSolid(MapColor.TERRACOTTA_BROWN, sound).strength(2.5F, 8.0F);
  }
}
