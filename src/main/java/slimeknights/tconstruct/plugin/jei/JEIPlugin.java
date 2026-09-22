package slimeknights.tconstruct.plugin.jei;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.google.common.collect.ImmutableList;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IModIdHelper;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.IRecipeManager;
import net.minecraft.core.RegistryAccess;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.tinkerstation.IDisplayToolTinkering;
import slimeknights.tconstruct.plugin.jei.util.TankHidingIngredientListener;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.fml.ModList;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.fluids.fluids.PotionFluidType;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe;
import slimeknights.tconstruct.library.recipe.casting.ICastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.IDisplayableCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.material.MaterialCastingLookup;
import slimeknights.tconstruct.library.recipe.display.FilteredRecipe;
import slimeknights.tconstruct.library.recipe.display.VanillaFilteredRecipe;
import slimeknights.tconstruct.library.recipe.entitymelting.EntityMeltingRecipe;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;
import slimeknights.tconstruct.library.recipe.material.IDisplayMaterialRecipe;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;
import slimeknights.tconstruct.library.recipe.material.ShapedMaterialRecipe;
import slimeknights.tconstruct.library.recipe.material.ShapedMaterialsRecipe;
import slimeknights.tconstruct.library.recipe.material.ShapelessMaterialsRecipe;
import slimeknights.tconstruct.library.recipe.melting.IDisplayableMeltingRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.ModifierRecipeLookup;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.adding.OverslimeCraftingTableRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.severing.SeveringRecipe;
import slimeknights.tconstruct.library.recipe.molding.MoldingRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.IDisplayPartBuilderRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.recipe.tinkerstation.AbstractCraftingTinkeringRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipe;
import slimeknights.tconstruct.library.recipe.worktable.IModifierWorktableRecipe;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.SlotType.SlotCount;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolTraitHook;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayoutLoader;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.library.utils.TinkerRecipeHelper;
import slimeknights.tconstruct.plugin.jei.casting.CastingBasinCategory;
import slimeknights.tconstruct.plugin.jei.casting.CastingRecipeManager;
import slimeknights.tconstruct.plugin.jei.casting.CastingTableCategory;
import slimeknights.tconstruct.plugin.jei.entity.DefaultEntityMeltingRecipe;
import slimeknights.tconstruct.plugin.jei.entity.EntityMeltingRecipeCategory;
import slimeknights.tconstruct.plugin.jei.entity.SeveringCategory;
import slimeknights.tconstruct.plugin.jei.material.MaterialCategory;
import slimeknights.tconstruct.plugin.jei.material.MaterialIconIngredientRenderer;
import slimeknights.tconstruct.plugin.jei.material.MaterialIngredientHelper;
import slimeknights.tconstruct.plugin.jei.material.OverslimeRecipeExtension;
import slimeknights.tconstruct.plugin.jei.material.ShapedMaterialsExtension;
import slimeknights.tconstruct.plugin.jei.material.ShapelessMaterialsExtension;
import slimeknights.tconstruct.plugin.jei.melting.FoundryCategory;
import slimeknights.tconstruct.plugin.jei.melting.FuelCategory;
import slimeknights.tconstruct.plugin.jei.melting.MeltingCategory;
import slimeknights.tconstruct.plugin.jei.melting.MeltingFuelHandler;
import slimeknights.tconstruct.plugin.jei.modifiers.ModifierBookmarkIngredientRenderer;
import slimeknights.tconstruct.plugin.jei.modifiers.ModifierIngredientHelper;
import slimeknights.tconstruct.plugin.jei.modifiers.ModifierRecipeCategory;
import slimeknights.tconstruct.plugin.jei.modifiers.ModifierWorktableCategory;
import slimeknights.tconstruct.plugin.jei.modifiers.SlotIngredientHelper;
import slimeknights.tconstruct.plugin.jei.modifiers.SlotIngredientRenderer;
import slimeknights.tconstruct.plugin.jei.modifiers.ToolTinkeringCategory;
import slimeknights.tconstruct.plugin.jei.modifiers.ToolTinkeringExtension;
import slimeknights.tconstruct.plugin.jei.partbuilder.MaterialItemList;
import slimeknights.tconstruct.plugin.jei.partbuilder.PartBuilderCategory;
import slimeknights.tconstruct.plugin.jei.partbuilder.PatternIngredientHelper;
import slimeknights.tconstruct.plugin.jei.partbuilder.PatternIngredientRenderer;
import slimeknights.tconstruct.plugin.jei.transfer.CraftingStationTransferInfo;
import slimeknights.tconstruct.plugin.jei.transfer.TinkerStationTransferInfo;
import slimeknights.tconstruct.plugin.jei.transfer.ToolInventoryTransferInfo;
import slimeknights.tconstruct.plugin.jei.util.GuiContainerTankHandler;
import slimeknights.tconstruct.plugin.jei.util.PotionSubtypeInterpreter;
import slimeknights.tconstruct.plugin.jei.util.ToolPartSubtypeInterpreter;
import slimeknights.tconstruct.plugin.jei.util.ToolSubtypeInterpreter;
import slimeknights.tconstruct.plugin.jei.util.manager.SimpleItemRecipeManager;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock.TankType;
import slimeknights.tconstruct.smeltery.client.screen.AlloyerScreen;
import slimeknights.tconstruct.smeltery.client.screen.HeatingStructureScreen;
import slimeknights.tconstruct.smeltery.client.screen.MelterScreen;
import slimeknights.tconstruct.smeltery.item.CopperCanItem;
import slimeknights.tconstruct.smeltery.item.TankItem;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.recipe.CraftingTableRepairKitRecipe;
import slimeknights.tconstruct.tables.recipe.TinkerStationRepairRecipe;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.client.ToolContainerScreen;
import slimeknights.tconstruct.tools.item.CreativeSlotItem;
import slimeknights.tconstruct.tools.item.ModifierCrystalItem;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@JeiPlugin
public class JEIPlugin implements IModPlugin {
  private static final Codec<ModifierEntry> MODIFIER_CODEC = RecordCodecBuilder.create(instance -> instance.group(
    Identifier.CODEC.xmap(ModifierId::new, ModifierId::location).fieldOf("id").forGetter(ModifierEntry::getId),
    Codec.INT.fieldOf("level").forGetter(ModifierEntry::getLevel)
  ).apply(instance, ModifierEntry::new));
  private static final Codec<Pattern> PATTERN_CODEC = Identifier.CODEC.xmap(Pattern::new, Pattern::location);
  private static final Codec<MaterialVariant> MATERIAL_CODEC = Codec.STRING.comapFlatMap(value -> {
    MaterialVariantId id = MaterialVariantId.tryParse(value);
    return id == null ? com.mojang.serialization.DataResult.error(() -> "Invalid material: " + value)
                      : com.mojang.serialization.DataResult.success(MaterialVariant.of(id));
  }, material -> material.getVariant().toString());
  private static final Codec<SlotCount> SLOT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
    Codec.STRING.xmap(SlotType::getOrCreate, SlotType::getName).fieldOf("type").forGetter((SlotCount slots) -> slots.type()),
    Codec.INT.fieldOf("count").forGetter(SlotCount::count)
  ).apply(instance, SlotCount::new));

  /** Recipes that are meant as jokes and tend to confuse players, so are hidden */
  private static final Identifier[] EASTER_EGG_RECIPES = {
    TConstruct.getResource("tables/tinkers_forge"),
    TConstruct.getResource("tables/scorched_forge"),
    TConstruct.getResource("tables/seared_forge_material"),
    TConstruct.getResource("tables/scorched_forge_material")
  };
  /** @deprecated no longer used */
  @Deprecated(forRemoval = true)
  public static IModIdHelper modIdHelper;

  /**
   * Adds the owning mod to a custom ingredient tooltip when JEI's global mod-name line is disabled.
   * Item stack tooltips are handled by the platform or other tooltip providers, but custom JEI
   * ingredients have no equivalent fallback.
   */
  public static void addCustomIngredientModName(ITooltipBuilder tooltip, Identifier identifier) {
    IModIdHelper helper = modIdHelper;
    if (helper != null && !helper.isDisplayingModNameEnabled()) {
      String namespace = identifier.getNamespace();
      String translationKey = "jade.modName." + namespace;
      String modName = I18n.exists(translationKey) ? I18n.get(translationKey) : helper.getModNameForModId(namespace);
      tooltip.add(Component.literal(modName)
                           .withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
    }
  }

  @Override
  public Identifier getPluginUid() {
    return TConstructJEIConstants.PLUGIN;
  }

  @Override
  public void registerCategories(IRecipeCategoryRegistration registry) {
    final IGuiHelper guiHelper = registry.getJeiHelpers().getGuiHelper();
    // casting
    registry.addRecipeCategories(new CastingBasinCategory(guiHelper));
    registry.addRecipeCategories(new CastingTableCategory(guiHelper));
    registry.addRecipeCategories(new MoldingRecipeCategory(guiHelper));
    // melting and casting
    registry.addRecipeCategories(new MeltingCategory(guiHelper));
    registry.addRecipeCategories(new AlloyRecipeCategory(guiHelper));
    registry.addRecipeCategories(new EntityMeltingRecipeCategory(guiHelper));
    registry.addRecipeCategories(new FoundryCategory(guiHelper));
    registry.addRecipeCategories(new FuelCategory(guiHelper));
    // tinker station
    registry.addRecipeCategories(new ModifierRecipeCategory(guiHelper));
    registry.addRecipeCategories(new SeveringCategory(guiHelper));
    registry.addRecipeCategories(new ToolBuildingCategory(guiHelper));
    registry.addRecipeCategories(new ToolTinkeringCategory(guiHelper));
    ToolTinkeringExtension.prepareDrawables(guiHelper);
    // part builder
    registry.addRecipeCategories(new MaterialCategory(guiHelper));
    registry.addRecipeCategories(new PartBuilderCategory(guiHelper));
    // modifier worktable
    registry.addRecipeCategories(new ModifierWorktableCategory(guiHelper));
  }

  @Override
  public void registerIngredients(IModIngredientRegistration registration) {
    List<ModifierEntry> modifiers = Collections.emptyList();
    if (Config.CLIENT.showModifiersInJEI.get()) {
      modifiers = ModifierRecipeLookup.getRecipeModifierList();
    }
    registration.register(TConstructJEIConstants.MODIFIER_TYPE, modifiers, new ModifierIngredientHelper(), ModifierBookmarkIngredientRenderer.INSTANCE, MODIFIER_CODEC);
    registration.register(TConstructJEIConstants.PATTERN_TYPE, Collections.emptyList(), new PatternIngredientHelper(), PatternIngredientRenderer.INSTANCE, PATTERN_CODEC);
    registration.register(TConstructJEIConstants.MATERIAL_TYPE, List.of(), new MaterialIngredientHelper(), MaterialIconIngredientRenderer.INSTANCE, MATERIAL_CODEC);
    List<SlotCount> slots = SlotType.getAllSlotTypes().stream().map(type -> new SlotCount(type, 1)).toList();
    SlotIngredientRenderer.clearCache();
    registration.register(TConstructJEIConstants.SLOT_TYPE, slots, new SlotIngredientHelper(), SlotIngredientRenderer.INGREDIENT, SLOT_CODEC);
  }

  @Override
  public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registry) {
    var crafting = registry.getCraftingCategory();
    crafting.addExtension(ShapedMaterialRecipe.class, ShapedMaterialExtension.INSTANCE);
    crafting.addExtension(ShapedMaterialsRecipe.class, ShapedMaterialsExtension.INSTANCE);
    crafting.addExtension(ShapelessMaterialsRecipe.class, ShapelessMaterialsExtension.INSTANCE);
    crafting.addExtension(OverslimeCraftingTableRecipe.class, OverslimeRecipeExtension.INSTANCE);
    crafting.addExtension(AbstractCraftingTinkeringRecipe.class, ToolTinkeringExtension.INSTANCE);
  }

  private static List<IDisplayableCastingRecipe> getCastingRecipes(RegistryAccess access, RecipeManager manager, Supplier<? extends RecipeType<ICastingRecipe>> recipeType) {
    return FilteredRecipe.unfiltered(TinkerRecipeHelper.getJEIRecipes(access, manager, recipeType.get(), IDisplayableCastingRecipe.class));
  }

  /** Finds the first recipe of the given type */
  @Nullable
  private static <T, C extends net.minecraft.world.item.crafting.RecipeInput, R extends Recipe<C>> T findFirst(RecipeManager manager, RecipeType<R> type, Class<T> clazz) {
    return TinkerRecipeHelper.getAllRecipesFor(manager, type).stream().map(RecipeHolder::value).filter(clazz::isInstance).map(clazz::cast).findFirst().orElse(null);
  }

  @Override
  public void registerRecipes(IRecipeRegistration register) {
    Level level = Minecraft.getInstance().level;
    assert level != null;
    RegistryAccess access = level.registryAccess();
    RecipeManager manager = slimeknights.tconstruct.library.utils.TinkerRecipeHelper.getRecipeManager(level);
    // casting
    register.addRecipes(TConstructJEIConstants.CASTING_BASIN, getCastingRecipes(access, manager, TinkerRecipeTypes.CASTING_BASIN));
    register.addRecipes(TConstructJEIConstants.CASTING_TABLE, getCastingRecipes(access, manager, TinkerRecipeTypes.CASTING_TABLE));

    // melting
    // need to register the fuels before the categories so they are available
    MeltingFuelHandler.registerSolidFuels(register.getIngredientManager());
    List<MeltingFuel> fuels = TinkerRecipeHelper.getRecipes(manager, TinkerRecipeTypes.FUEL.get(), MeltingFuel.class);
    MeltingFuelHandler.setMeltngFuels(fuels);
    register.addRecipes(TConstructJEIConstants.FUEL, fuels);
    // register melting recipes
    List<IDisplayableMeltingRecipe> meltingRecipes = TinkerRecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.MELTING.get(), IDisplayableMeltingRecipe.class);
    register.addRecipes(TConstructJEIConstants.MELTING, meltingRecipes);
    register.addRecipes(TConstructJEIConstants.FOUNDRY, meltingRecipes);

    // entity melting
    List<EntityMeltingRecipe> entityMeltingRecipes = new ArrayList<>(TinkerRecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.ENTITY_MELTING.get(), EntityMeltingRecipe.class));
    // generate a "default" recipe for all other entity types
    entityMeltingRecipes.add(new DefaultEntityMeltingRecipe(entityMeltingRecipes));
    register.addRecipes(TConstructJEIConstants.ENTITY_MELTING, entityMeltingRecipes);

    // alloying
    List<AlloyRecipe> alloyRecipes = TinkerRecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.ALLOYING.get(), AlloyRecipe.class);
    register.addRecipes(TConstructJEIConstants.ALLOY, alloyRecipes);
    TConstruct.LOG.info("Registered smeltery JEI recipes: melting={}, fuel={}, entity_melting={}, alloying={}",
      meltingRecipes.size(),
      TinkerRecipeHelper.getRecipes(manager, TinkerRecipeTypes.FUEL.get(), MeltingFuel.class).size(),
      entityMeltingRecipes.size(),
      alloyRecipes.size());

    // molding
    List<MoldingRecipe> moldingRecipes = new ArrayList<>();
    moldingRecipes.addAll(TinkerRecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.MOLDING_TABLE.get(), MoldingRecipe.class));
    moldingRecipes.addAll(TinkerRecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.MOLDING_BASIN.get(), MoldingRecipe.class));
    register.addRecipes(TConstructJEIConstants.MOLDING, moldingRecipes);

    // modifiers
    List<IDisplayModifierRecipe> modifierRecipes = TinkerRecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.TINKER_STATION.get(), IDisplayModifierRecipe.class)
                                                               .stream()
                                                               .sorted((r1, r2) -> {
                                                                 SlotType t1 = r1.getSlotType();
                                                                 SlotType t2 = r2.getSlotType();
                                                                 String n1 = t1 == null ? "zzzzzzzzzz" : t1.getName();
                                                                 String n2 = t2 == null ? "zzzzzzzzzz" : t2.getName();
                                                                 return n1.compareTo(n2);
                                                               }).collect(Collectors.toList());
    register.addRecipes(TConstructJEIConstants.MODIFIERS, modifierRecipes);
    register.addRecipes(TConstructJEIConstants.TOOL_MODIFICATION, FilteredRecipe.unfiltered(TinkerRecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.TINKER_STATION.get(), IDisplayToolTinkering.class)));

    // beheading
    List<SeveringRecipe> severingRecipes = TinkerRecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.SEVERING.get(), SeveringRecipe.class);
    register.addRecipes(TConstructJEIConstants.SEVERING, severingRecipes);

    // tool building
    List<ToolBuildingRecipe> toolBuilding = TinkerRecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.TINKER_STATION.get(), ToolBuildingRecipe.class)
      .stream()
      .sorted(Comparator.comparingInt(r -> StationSlotLayoutLoader.getInstance().get(r.getLayoutSlotId()).getSortIndex()))
      .toList();
    register.addRecipes(TConstructJEIConstants.TOOL_BUILDING, toolBuilding);

    // materials
    List<IDisplayMaterialRecipe> materialRecipes = Stream.<IDisplayMaterialRecipe>concat(
        MaterialRecipeCache.getSortedRecipes().stream(),
        Stream.concat(MaterialCastingLookup.getSortedCastingFluids().stream(), MaterialCastingLookup.getSortedCompositeFluids().stream()))
      .sorted(Comparator.comparing(IDisplayMaterialRecipe::getMaterial))
      // A client recipe reload can expose the same data-pack recipe through both
      // the synchronized recipe map and the material cache. Keep one card per id.
      .collect(java.util.stream.Collectors.toMap(r -> r.getRecipeId() + "|" + r.getMaterial() + "|" + r.getDisplayItems() + "|" + r.getFluids(), r -> r,
        (first, ignored) -> first, LinkedHashMap::new)).values().stream().toList();
    register.addRecipes(TConstructJEIConstants.MATERIALS, materialRecipes);

    // part builder
    MaterialItemList.setRecipes(List.of()); // list of recipes is ignored as this whole class is getting ditched in 1.21; it just clears cache right now
    List<IDisplayPartBuilderRecipe> partBuilderRecipes = TinkerRecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.PART_BUILDER.get(), IDisplayPartBuilderRecipe.class);
    register.addRecipes(TConstructJEIConstants.PART_BUILDER, partBuilderRecipes);

    // modifier worktable
    register.addRecipes(TConstructJEIConstants.MODIFIER_WORKTABLE, TinkerRecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.MODIFIER_WORKTABLE.get(), IModifierWorktableRecipe.class)
      .stream().filter(recipe -> {
        if (recipe.getModifierOptions(null).isEmpty()) {
          TConstruct.LOG.debug("Hiding Modifier Worktable recipe {} as it has no valid modifiers", recipe);
          return false;
        }
        return true;
      }).toList());

    // copy tinker station repair recipes to the crafting table
    RecipeHolder<CraftingRecipe> craftingRepair = TinkerRecipeHelper.getAllRecipesFor(manager, RecipeType.CRAFTING).stream()
      .filter(holder -> holder.value().getClass() == CraftingTableRepairKitRecipe.class).findFirst().orElse(null);
    TinkerStationRepairRecipe tinkerRepair = findFirst(manager, TinkerRecipeTypes.TINKER_STATION.get(), TinkerStationRepairRecipe.class);
    if (craftingRepair != null && tinkerRepair != null) {
      Identifier id = craftingRepair.id().identifier();
      List<AbstractCraftingTinkeringRecipe> recipes = tinkerRepair.getRecipes(access);
      List<RecipeHolder<CraftingRecipe>> newRecipes = new ArrayList<>();
      TinkerStationRepairRecipe.setCraftingId(recipes, id);
      for (AbstractCraftingTinkeringRecipe recipe : recipes) {
        if (!recipe.isFiltered()) {
          newRecipes.add(new RecipeHolder<>(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, recipe.getId()), recipe));
        }
      }
      if (!recipes.isEmpty()) {
        register.addRecipes(RecipeTypes.CRAFTING, newRecipes);
      }
    }

    // add an ingredient listener to hide tanks when fluids are hidden
    IIngredientManager ingredientManager = register.getIngredientManager();
    ingredientManager.registerIngredientListener(new TankHidingIngredientListener(ingredientManager, Stream.concat(
      Stream.of(TinkerSmeltery.copperCan, TinkerSmeltery.searedLantern, TinkerSmeltery.scorchedLantern),
      Stream.concat(TinkerSmeltery.searedTank.values().stream(), TinkerSmeltery.scorchedTank.values().stream())
    ).map(ItemLike::asItem).toList()));
  }

  /** Gets a list of filtered casting recipes to give to JEI. */
  private static List<IDisplayableCastingRecipe> getFilteredCastingRecipes(RegistryAccess access, RecipeManager manager, Supplier<? extends RecipeType<ICastingRecipe>> recipeType) {
    return FilteredRecipe.filtered(TinkerRecipeHelper.getJEIRecipes(access, manager, recipeType.get(), IDisplayableCastingRecipe.class));
  }

  @Override
  public void registerAdvanced(IAdvancedRegistration registration) {
    Level level = Minecraft.getInstance().level;
    assert level != null;
    RegistryAccess access = level.registryAccess();
    RecipeManager manager = TinkerRecipeHelper.getRecipeManager(level);

    IIngredientManager ingredientManager = registration.getJeiHelpers().getIngredientManager();
    registration.addTypedRecipeManagerPlugin(TConstructJEIConstants.CASTING_BASIN, new CastingRecipeManager(ingredientManager, getFilteredCastingRecipes(access, manager, TinkerRecipeTypes.CASTING_BASIN)));
    registration.addTypedRecipeManagerPlugin(TConstructJEIConstants.CASTING_TABLE, new CastingRecipeManager(ingredientManager, getFilteredCastingRecipes(access, manager, TinkerRecipeTypes.CASTING_TABLE)));
    registration.addTypedRecipeManagerPlugin(TConstructJEIConstants.TOOL_MODIFICATION, new SimpleItemRecipeManager<>(ingredientManager, FilteredRecipe.filtered(TinkerRecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.TINKER_STATION.get(), IDisplayToolTinkering.class))));

    // copy tinker station repair recipes to the crafting table
    List<AbstractCraftingTinkeringRecipe> craftingRecipes = VanillaFilteredRecipe.getRecipes(access, manager, RecipeType.CRAFTING, AbstractCraftingTinkeringRecipe.class);
    // copy tinker repair recipes to the crafting table
    TinkerStationRepairRecipe tinkerRepair = findFirst(manager, TinkerRecipeTypes.TINKER_STATION.get(), TinkerStationRepairRecipe.class);
    if (tinkerRepair != null) {
      List<AbstractCraftingTinkeringRecipe> recipes = tinkerRepair.getRecipes(access);
      for (AbstractCraftingTinkeringRecipe recipe : recipes) {
        if (recipe.isFiltered()) {
          craftingRecipes.add(recipe);
        }
      }
    }
    // add the plugin if we found anything
    if (!craftingRecipes.isEmpty()) {
      registration.addTypedRecipeManagerPlugin(RecipeTypes.CRAFTING, SimpleItemRecipeManager.createCrafting(ingredientManager, craftingRecipes));
    }
  }

  /** Adds a table as a catalys */
  private static void addTableCatalyst(IRecipeCatalystRegistration registry, ItemLike table, TagKey<Item> tag, boolean addDefault, IRecipeType<?>... types) {
    // JEI 29 expands every stack in addCraftingStations into a separate icon.
    // Use the base table as the representative catalyst; recipe lookup remains
    // available from the actual retextured table item in the recipe UI.
    List<ItemStack> list = List.of(new ItemStack(table));
    for (IRecipeType<?> type : types) {
      registry.addCraftingStations(type, VanillaTypes.ITEM_STACK, list);
    }
  }

  /**
   * Adds an item as a casting catalyst, and as a molding catalyst if it has molding recipes
   * @param registry     Catalyst regisry
   * @param item         Item to add
   * @param ownCategory  Category to always add
   * @param type         Molding recipe type
   */
  private static <T extends Recipe<C>, C extends RecipeInput> void addCastingCatalyst(IRecipeCatalystRegistration registry, ItemLike item, mezz.jei.api.recipe.RecipeType<IDisplayableCastingRecipe> ownCategory, RecipeType<MoldingRecipe> type) {
    ItemStack stack = new ItemStack(item);
    registry.addRecipeCatalyst(stack, ownCategory);
    assert Minecraft.getInstance().level != null;
    if (!slimeknights.tconstruct.library.utils.TinkerRecipeHelper.getAllRecipesFor(slimeknights.tconstruct.library.utils.TinkerRecipeHelper.getRecipeManager(Minecraft.getInstance().level),type).isEmpty()) {
      registry.addRecipeCatalyst(stack, TConstructJEIConstants.MOLDING);
    }
  }

  /** Adds all entries from the given modifier tag as catalysts for the given recipe types. */
  private static void addModifierCatalyst(IRecipeCatalystRegistration registry, TagKey<Modifier> tag, IRecipeType<?>... types) {
    for (Modifier modifier : ModifierManager.getTagValues(tag)) {
      registry.addRecipeCatalyst(TConstructJEIConstants.MODIFIER_TYPE, new ModifierEntry(modifier, 1), types);
    }
  }

  @Override
  public void registerRecipeCatalysts(IRecipeCatalystRegistration registry) {
    // tables
    addTableCatalyst(registry, TinkerTables.craftingStation, ItemTags.LOGS, true, RecipeTypes.CRAFTING);
    addTableCatalyst(registry, TinkerTables.partBuilder, ItemTags.PLANKS, true, TConstructJEIConstants.PART_BUILDER);
    addTableCatalyst(registry, TinkerTables.tinkerStation, ItemTags.PLANKS, true, TConstructJEIConstants.MODIFIERS, TConstructJEIConstants.TOOL_BUILDING, TConstructJEIConstants.TOOL_MODIFICATION);
    addTableCatalyst(registry, TinkerTables.tinkersAnvil, TinkerTags.Items.ANVIL_METAL, false, TConstructJEIConstants.MODIFIERS, TConstructJEIConstants.TOOL_BUILDING, TConstructJEIConstants.TOOL_MODIFICATION);
    addTableCatalyst(registry, TinkerTables.scorchedAnvil, TinkerTags.Items.ANVIL_METAL, false, TConstructJEIConstants.MODIFIERS, TConstructJEIConstants.TOOL_BUILDING, TConstructJEIConstants.TOOL_MODIFICATION);
    addTableCatalyst(registry, TinkerTables.modifierWorktable, TinkerTags.Items.WORKSTATION_ROCK, true, TConstructJEIConstants.MODIFIER_WORKTABLE);

    // smeltery
    registry.addRecipeCatalyst(TinkerSmeltery.searedMelter, TConstructJEIConstants.MELTING, TConstructJEIConstants.FUEL);
    registry.addRecipeCatalyst(TinkerSmeltery.searedHeater, RecipeTypes.SMELTING_FUEL, TConstructJEIConstants.FUEL);
    addCastingCatalyst(registry, TinkerSmeltery.searedTable, TConstructJEIConstants.CASTING_TABLE, TinkerRecipeTypes.MOLDING_TABLE.get());
    addCastingCatalyst(registry, TinkerSmeltery.searedBasin, TConstructJEIConstants.CASTING_BASIN, TinkerRecipeTypes.MOLDING_BASIN.get());
    addTableCatalyst(registry, TinkerSmeltery.smelteryController, TinkerTags.Items.SEARED_BLOCKS, false, TConstructJEIConstants.MELTING, TConstructJEIConstants.ALLOY, TConstructJEIConstants.ENTITY_MELTING, TConstructJEIConstants.FUEL);

    // foundry
    registry.addRecipeCatalyst(TinkerSmeltery.scorchedAlloyer, TConstructJEIConstants.ALLOY, TConstructJEIConstants.FUEL);
    addCastingCatalyst(registry, TinkerSmeltery.scorchedTable, TConstructJEIConstants.CASTING_TABLE, TinkerRecipeTypes.MOLDING_TABLE.get());
    addCastingCatalyst(registry, TinkerSmeltery.scorchedBasin, TConstructJEIConstants.CASTING_BASIN, TinkerRecipeTypes.MOLDING_BASIN.get());
    addTableCatalyst(registry, TinkerSmeltery.foundryController, TinkerTags.Items.SCORCHED_BLOCKS, false, TConstructJEIConstants.FOUNDRY, TConstructJEIConstants.FUEL);

    // modifiers
    addModifierCatalyst(registry, TinkerTags.Modifiers.CRAFTING, RecipeTypes.CRAFTING);
    addModifierCatalyst(registry, TinkerTags.Modifiers.SMELTING, RecipeTypes.SMELTING);
    addModifierCatalyst(registry, TinkerTags.Modifiers.SEVERING, TConstructJEIConstants.SEVERING);
    addModifierCatalyst(registry, TinkerTags.Modifiers.MELTING, TConstructJEIConstants.MELTING, TConstructJEIConstants.ENTITY_MELTING);
    for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(TinkerTags.Items.MODIFIABLE)) {
      if (item.value() instanceof IModifiableDisplay modifiable) {
        // add any tools with a severing trait to severing
        ModifierNBT traits = ToolTraitHook.getTraits(modifiable.getToolDefinition(), MaterialNBT.EMPTY);
        if (traits.has(TinkerTags.Modifiers.CRAFTING)) {
          registry.addRecipeCatalyst(modifiable.getRenderTool(), RecipeTypes.CRAFTING);
        }
        if (traits.has(TinkerTags.Modifiers.SMELTING)) {
          registry.addRecipeCatalyst(modifiable.getRenderTool(), RecipeTypes.SMELTING);
        }
        if (traits.has(TinkerTags.Modifiers.SEVERING)) {
          registry.addRecipeCatalyst(modifiable.getRenderTool(), TConstructJEIConstants.SEVERING);
        }
        // add any tools with a melting trait to melting
        if (traits.has(TinkerTags.Modifiers.MELTING)) {
          // only add to entity melting if its melee too
          if (item.is(TinkerTags.Items.MELEE)) {
            registry.addRecipeCatalyst(modifiable.getRenderTool(), TConstructJEIConstants.MELTING, TConstructJEIConstants.ENTITY_MELTING);
          } else {
            registry.addRecipeCatalyst(modifiable.getRenderTool(), TConstructJEIConstants.MELTING);
          }
        }
      }
    }
  }

  @Override
  public void registerItemSubtypes(ISubtypeRegistration registry) {
    // retexturable blocks
    ISubtypeInterpreter<ItemStack> tables = (stack, context) -> {
      if (context == UidContext.Ingredient) {
        return RetexturedHelper.getTextureName(stack);
      }
      return null;
    };
    registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, TinkerTables.craftingStation.asItem(), tables);
    registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, TinkerTables.partBuilder.asItem(), tables);
    registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, TinkerTables.tinkerStation.asItem(), tables);
    registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, TinkerTables.modifierWorktable.asItem(), tables);
    registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, TinkerSmeltery.smelteryController.asItem(), tables);
    registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, TinkerSmeltery.searedDrain.asItem(), tables);
    registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, TinkerSmeltery.searedDuct.asItem(), tables);
    registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, TinkerSmeltery.searedChute.asItem(), tables);
    registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, TinkerSmeltery.foundryController.asItem(), tables);
    registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, TinkerSmeltery.scorchedDrain.asItem(), tables);
    registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, TinkerSmeltery.scorchedDuct.asItem(), tables);
    registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, TinkerSmeltery.scorchedChute.asItem(), tables);

    // anvils have both texture and material blocks
    ISubtypeInterpreter<ItemStack> anvils = (stack, context) -> {
      if (context == UidContext.Ingredient) {
        String name = RetexturedHelper.getTextureName(stack);
        if (!name.isEmpty()) {
          return '#' + name;
        }
        return ToolPartSubtypeInterpreter.INSTANCE.getSubtypeData(stack, UidContext.Ingredient);
      }
      return null;
    };
    registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, TinkerTables.tinkersAnvil.asItem(), anvils);
    registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, TinkerTables.scorchedAnvil.asItem(), anvils);

    // potions
    registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, TinkerFluids.potion.asItem(), (PotionSubtypeInterpreter<ItemStack>)PotionFluidType::getPotionContents);
    registry.registerSubtypeInterpreter(NeoForgeTypes.FLUID_STACK, TinkerFluids.potion.get(), (PotionSubtypeInterpreter<FluidStack>)PotionFluidType::getPotionContents);

    // parts
    for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(TinkerTags.Items.TOOL_PARTS)) {
      registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, item.value(), ToolPartSubtypeInterpreter.INSTANCE);
    }

    // tools
    for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(TinkerTags.Items.MULTIPART_TOOL)) {
      Item item = holder.value();
      registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, item, holder.is(TinkerTags.Items.SINGLEPART_TOOL) ? ToolSubtypeInterpreter.FIRST : ToolSubtypeInterpreter.INGREDIENT);
    }

    // fluid containers have types based on fluid, don't bother with different sizes
    registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, TinkerSmeltery.copperCan.get(), (stack, context) -> CopperCanItem.getSubtype(stack));
    ISubtypeInterpreter<ItemStack> tankInterpreter = (stack, context) -> TankItem.getSubtype(stack);
    for (TankType type : TankType.values()) {
      registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, TinkerSmeltery.searedTank.get(type).asItem(), tankInterpreter);
      registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, TinkerSmeltery.scorchedTank.get(type).asItem(), tankInterpreter);
    }
    registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, TinkerSmeltery.searedLantern.asItem(), tankInterpreter);
    registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, TinkerSmeltery.scorchedLantern.asItem(), tankInterpreter);
    registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, TinkerSmeltery.searedFluidCannon.asItem(), tankInterpreter);
    registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, TinkerSmeltery.scorchedFluidCannon.asItem(), tankInterpreter);
    registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, TinkerSmeltery.endFluidCannon.asItem(), tankInterpreter);

    registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, TinkerModifiers.creativeSlotItem.get(), (stack, context) -> {
      SlotType slotType = CreativeSlotItem.getSlot(stack);
      return slotType != null ? slotType.getName() : "";
    });
    registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, TinkerModifiers.modifierCrystal.get(), (stack, context) -> {
      ModifierId id = ModifierCrystalItem.getModifier(stack);
      return id == null ? "" : id.toString();
    });
  }

  @Override
  public void registerGuiHandlers(IGuiHandlerRegistration registration) {
    registration.addGenericGuiContainerHandler(MelterScreen.class, new GuiContainerTankHandler<>());
    registration.addGenericGuiContainerHandler(AlloyerScreen.class, new GuiContainerTankHandler<>());
    registration.addGenericGuiContainerHandler(HeatingStructureScreen.class, new GuiContainerTankHandler<>());
    registration.addGenericGuiContainerHandler(ToolContainerScreen.class, new GuiContainerTankHandler<>());
  }

  @Override
  public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
    registration.addRecipeTransferHandler(new CraftingStationTransferInfo());
    IRecipeTransferHandlerHelper helper = registration.getTransferHelper();
    registration.addRecipeTransferHandler(new TinkerStationTransferInfo<>(TConstructJEIConstants.MODIFIERS, helper), TConstructJEIConstants.MODIFIERS);
    registration.addRecipeTransferHandler(new TinkerStationTransferInfo<>(TConstructJEIConstants.TOOL_BUILDING, helper), TConstructJEIConstants.TOOL_BUILDING);
    registration.addRecipeTransferHandler(new ToolInventoryTransferInfo(helper), RecipeTypes.CRAFTING);
  }

  /**
   * Removes a fluid from JEI
   * @param remove  List of ingredients to remove for batching
   * @param fluid   Fluid to remove
   */
  private static void removeFluid(List<FluidStack> remove, Fluid fluid) {
    remove.add(new FluidStack(fluid, FluidType.BUCKET_VOLUME));
  }

  @Override
  public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
    IIngredientManager manager = jeiRuntime.getIngredientManager();

    List<ItemStack> removeItems = new ArrayList<>();
    Consumer<ItemStack> removeItem = removeItems::add;
    List<ItemStack> addItems = new ArrayList<>();
    Consumer<ItemStack> addItem = addItems::add;
    // shown via the modifiers
    removeItems.add(new ItemStack(TinkerModifiers.modifierCrystal));
    ModifierCrystalItem.addVariants(removeItem);
    // shown via modifier slots
    removeItems.add(new ItemStack(TinkerModifiers.creativeSlotItem));
    TinkerModifiers.creativeSlotItem.get().addVariants(removeItem);

    // tool config filters to 1 material, easiest to just remove all then add back the 1
    String showOnlyTools = Config.CLIENT.showOnlyToolMaterial.get();
    if (!showOnlyTools.isEmpty()) {
      for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(TinkerTags.Items.MODIFIABLE)) {
        if (item.value() instanceof IModifiable modifiable) {
          ToolBuildHandler.addVariants(removeItem, modifiable, "");
          ToolBuildHandler.addVariants(addItem, modifiable, showOnlyTools);
        }
      }
    }
    String showOnlyParts = Config.CLIENT.showOnlyPartMaterial.get();
    if (!showOnlyParts.isEmpty()) {
      for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(TinkerTags.Items.TOOL_PARTS)) {
        if (item.value() instanceof IMaterialItem part) {
          part.addVariants(removeItem, "");
          part.addVariants(addItem, showOnlyParts);
        }
      }
    }
    // The decoration and tank creative tabs are hidden from JEI, so add requested variants explicitly.
    Predicate<ItemStack> addVariant = stack -> {
      addItems.add(stack);
      return false;
    };
    if (Config.CLIENT.showAllTableVariants.get()) {
      RetexturedHelper.addTagVariants(addVariant, TinkerTables.craftingStation, ItemTags.LOGS);
      RetexturedHelper.addTagVariants(addVariant, TinkerTables.partBuilder, ItemTags.PLANKS);
      RetexturedHelper.addTagVariants(addVariant, TinkerTables.tinkerStation, ItemTags.PLANKS);
      RetexturedHelper.addTagVariants(addVariant, TinkerTables.modifierWorktable, TinkerTags.Items.WORKSTATION_ROCK);
    }
    if (Config.CLIENT.showAllSmelteryVariants.get()) {
      RetexturedHelper.addTagVariants(addVariant, TinkerSmeltery.smelteryController, TinkerTags.Items.SEARED_BLOCKS);
      RetexturedHelper.addTagVariants(addVariant, TinkerSmeltery.searedDrain, TinkerTags.Items.SEARED_BLOCKS);
      RetexturedHelper.addTagVariants(addVariant, TinkerSmeltery.searedDuct, TinkerTags.Items.SEARED_BLOCKS);
      RetexturedHelper.addTagVariants(addVariant, TinkerSmeltery.searedChute, TinkerTags.Items.SEARED_BLOCKS);
      RetexturedHelper.addTagVariants(addVariant, TinkerSmeltery.foundryController, TinkerTags.Items.SCORCHED_BLOCKS);
      RetexturedHelper.addTagVariants(addVariant, TinkerSmeltery.scorchedDrain, TinkerTags.Items.SCORCHED_BLOCKS);
      RetexturedHelper.addTagVariants(addVariant, TinkerSmeltery.scorchedDuct, TinkerTags.Items.SCORCHED_BLOCKS);
      RetexturedHelper.addTagVariants(addVariant, TinkerSmeltery.scorchedChute, TinkerTags.Items.SCORCHED_BLOCKS);
    }
    if (Config.CLIENT.showAllAnvilVariants.get()) {
      ((IMaterialItem) TinkerTables.tinkersAnvil.asItem()).addVariants(addItem, "");
      ((IMaterialItem) TinkerTables.scorchedAnvil.asItem()).addVariants(addItem, "");
    }
    if (Config.CLIENT.showFilledFluidTanks.get()) {
      CopperCanItem.addFilledVariants(addItem);
      TankItem.addFilledVariants(addItem);
    }

    if (!removeItems.isEmpty()) {
      manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, removeItems);
    }
    if (!addItems.isEmpty()) {
      manager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, addItems);
    }

    // fluid hiding, buckets are hidden via the creative tab logic
    // hide compat that is not present
    List<FluidStack> removeFluids = new ArrayList<>();
    List<ItemStack> removeBuckets = new ArrayList<>();
    for (slimeknights.tconstruct.smeltery.data.SmelteryCompat compat : slimeknights.tconstruct.smeltery.data.SmelteryCompat.values()) {
      if (!compat.isPresent()) {
        var fluid = compat.getFluid();
        removeBuckets.add(new ItemStack(fluid));
        removeFluid(removeFluids, fluid.get());
      }
    }
    if (!ModList.get().isLoaded("ceramics")) {
      removeBuckets.add(new ItemStack(TinkerFluids.moltenPorcelain));
      removeFluid(removeFluids, TinkerFluids.moltenPorcelain.get());
    }
    if (!removeBuckets.isEmpty()) manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, removeBuckets);

    // add potion fluids for each potion variant if requested
    if (Config.CLIENT.showPotionFluidInJEI.get()) {
      manager.addIngredientsAtRuntime(NeoForgeTypes.FLUID_STACK,
                                      StreamSupport.stream(BuiltInRegistries.POTION.asHolderIdMap().spliterator(), false)
                                        .filter(holder -> !holder.equals(Potions.WATER) && !holder.is(TinkerTags.Potions.HIDDEN_FLUID))
                                        .map(holder -> PotionFluidType.potionFluid(holder, FluidType.BUCKET_VOLUME))
                                        .toList());
    }
    // remove variantless potion fluid
    removeFluid(removeFluids, TinkerFluids.potion.get());

    // remove all the fluids
    manager.removeIngredientsAtRuntime(NeoForgeTypes.FLUID_STACK, removeFluids);

    // hide easter egg recipes
    Level level = SafeClientAccess.getLevel();
    if (level != null) {
      RecipeManager recipes = slimeknights.tconstruct.library.utils.TinkerRecipeHelper.getRecipeManager(level);
      List<RecipeHolder<CraftingRecipe>> easterEggs = Arrays.stream(EASTER_EGG_RECIPES)
        .map(id -> recipes.byKey(ResourceKey.create(Registries.RECIPE, id)))
        .flatMap(Optional::stream)
        .filter(holder -> holder.value() instanceof CraftingRecipe)
        .map(holder -> new RecipeHolder<>(holder.id(), (CraftingRecipe) holder.value()))
        .toList();
      if (!easterEggs.isEmpty()) {
        jeiRuntime.getRecipeManager().hideRecipes(RecipeTypes.CRAFTING, easterEggs);
      }
    }

    modIdHelper = jeiRuntime.getJeiHelpers().getModIdHelper();
  }

  @Override
  public void onRuntimeUnavailable() {
    modIdHelper = null;
  }
}
