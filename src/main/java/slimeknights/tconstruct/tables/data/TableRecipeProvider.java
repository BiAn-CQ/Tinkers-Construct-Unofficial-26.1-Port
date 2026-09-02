package slimeknights.tconstruct.tables.data;

import slimeknights.tconstruct.library.recipe.TinkerIngredients;
import slimeknights.tconstruct.library.recipe.MaterialTinkerIngredients;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.crafting.DifferenceIngredient;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.recipe.crafting.ShapedRetexturedRecipeBuilder;
import slimeknights.mantle.recipe.ingredient.ItemNameIngredient;
import slimeknights.mantle.recipe.data.ItemNameOutput;
import slimeknights.tconstruct.library.recipe.helper.SimpleRecipeOutput;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.data.BaseRecipeProvider;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicate;
import slimeknights.tconstruct.library.recipe.ingredient.MaterialIngredient;
import slimeknights.tconstruct.library.recipe.material.MaterialsConsumerBuilder;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.recipe.partbuilder.recycle.PartBuilderRecycleBuilder;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.recipe.TinkerStationDamagingRecipeBuilder;
import slimeknights.tconstruct.tables.recipe.TinkerStationPartSwappingBuilder;
import slimeknights.tconstruct.tools.TinkerToolParts;
import slimeknights.tconstruct.tools.TinkerTools;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class TableRecipeProvider extends BaseRecipeProvider {
  public TableRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
    super(registries, output);
  }

  @Override
  protected void buildTinkersRecipes(RecipeOutput consumer) {
    this.tableRecipes(consumer);
    this.damageRecipes(consumer);
    this.recyclingRecipes(consumer);
  }

  private void tableRecipes(RecipeOutput consumer) {
    String folder = "tables/";
    // pattern
    shaped(RecipeCategory.MISC, TinkerTables.pattern, 6)
      .define('s', ingredient(Tags.Items.RODS_WOODEN))
      .define('p', ingredient(ItemTags.PLANKS))
      .pattern("ps")
      .pattern("sp")
      .unlockedBy("has_item", has(Tags.Items.RODS_WOODEN))
      .save(consumer, recipeKey(prefix(TinkerTables.pattern, folder)));

    // book from patterns and slime
    shapeless(RecipeCategory.MISC, Items.BOOK)
                          .requires(Items.PAPER)
                          .requires(Items.PAPER)
                          .requires(Items.PAPER)
                          .requires(ingredient(Tags.Items.SLIME_BALLS))
                          .requires(TinkerTables.pattern)
                          .requires(TinkerTables.pattern)
                          .unlockedBy("has_item", has(TinkerTables.pattern))
                          .save(consumer, recipeKey(location(folder + "book_substitute")));

    // crafting station -> crafting table upgrade
    shaped(RecipeCategory.DECORATIONS, TinkerTables.craftingStation)
      .define('p', TinkerTables.pattern)
      .define('w', DifferenceIngredient.of(TinkerIngredients.of(TinkerTags.Items.WORKBENCHES), TinkerIngredients.of(TinkerTables.craftingStation.get())))
      .pattern("p")
      .pattern("w")
      .unlockedBy("has_item", has(TinkerTables.pattern))
      .save(consumer, recipeKey(prefix(TinkerTables.craftingStation, folder)));
    // station with log texture
    ShapedRetexturedRecipeBuilder.fromShaped(
      shaped(RecipeCategory.DECORATIONS, TinkerTables.craftingStation)
        .define('p', TinkerTables.pattern)
        .define('w', ingredient(ItemTags.LOGS))
        .pattern("p")
        .pattern("w")
        .unlockedBy("has_item", has(TinkerTables.pattern)))
      .setSource(TinkerIngredients.of(ItemTags.LOGS))
      .build(consumer, recipeKey(wrap(TinkerTables.craftingStation, folder, "_from_logs")));
    ShapedRetexturedRecipeBuilder.fromShaped(
      shaped(RecipeCategory.DECORATIONS, TinkerTables.craftingStation)
        .define('p', TinkerTables.pattern)
        .define('w', DifferenceIngredient.of(TinkerIngredients.of(TinkerTags.Items.TABLES), TinkerIngredients.of(TinkerTables.craftingStation.get())))
        .pattern("p")
        .pattern("w")
        .unlockedBy("has_item", has(TinkerTables.pattern)))
      .setSource(DifferenceIngredient.of(TinkerIngredients.of(TinkerTags.Items.TABLES), TinkerIngredients.of(TinkerTables.craftingStation.get())))
      .build(consumer, recipeKey(wrap(TinkerTables.craftingStation, folder, "_from_tables")));

    // part builder
    ShapedRetexturedRecipeBuilder.fromShaped(
      shaped(RecipeCategory.DECORATIONS, TinkerTables.partBuilder)
        .define('p', TinkerTables.pattern)
        .define('w', ingredient(TinkerTags.Items.PLANKLIKE))
        .pattern("pp")
        .pattern("ww")
        .unlockedBy("has_item", has(TinkerTables.pattern)))
      .setSource(TinkerIngredients.of(TinkerTags.Items.PLANKLIKE))
      .setMatchAll()
      .build(consumer, recipeKey(prefix(TinkerTables.partBuilder, folder)));

    // tinker station
    ShapedRetexturedRecipeBuilder.fromShaped(
      shaped(RecipeCategory.DECORATIONS, TinkerTables.tinkerStation)
        .define('p', TinkerTables.pattern)
        .define('w', ingredient(TinkerTags.Items.PLANKLIKE))
        .pattern("ppp")
        .pattern("w w")
        .pattern("w w")
        .unlockedBy("has_item", has(TinkerTables.pattern)))
      .setSource(TinkerIngredients.of(TinkerTags.Items.PLANKLIKE))
      .setMatchAll()
      .build(consumer, recipeKey(prefix(TinkerTables.tinkerStation, folder)));

    // part chest
    shaped(RecipeCategory.DECORATIONS, TinkerTables.partChest)
                       .define('p', TinkerTables.pattern)
                       .define('w', ingredient(ItemTags.PLANKS))
                       .define('s', ingredient(Tags.Items.RODS_WOODEN))
                       .define('C', ingredient(Tags.Items.CHESTS_WOODEN))
                       .pattern(" p ")
                       .pattern("sCs")
                       .pattern("sws")
                       .unlockedBy("has_item", has(TinkerTables.pattern))
                       .save(consumer, recipeKey(prefix(TinkerTables.partChest, folder)));
    // modifier chest
    shaped(RecipeCategory.DECORATIONS, TinkerTables.tinkersChest)
                       .define('p', TinkerTables.pattern)
                       .define('w', ingredient(ItemTags.PLANKS))
                       .define('l', ingredient(Tags.Items.GEMS_LAPIS))
                       .define('C', ingredient(Tags.Items.CHESTS_WOODEN))
                       .pattern(" p " )
                       .pattern("lCl")
                       .pattern("lwl")
                       .unlockedBy("has_item", has(TinkerTables.pattern))
                       .save(consumer, recipeKey(prefix(TinkerTables.tinkersChest, folder)));
    // cast chest
    shaped(RecipeCategory.DECORATIONS, TinkerTables.castChest)
                       .define('c', ingredient(TinkerTags.Items.GOLD_CASTS))
                       .define('b', TinkerSmeltery.searedBrick)
                       .define('B', TinkerSmeltery.searedBricks)
                       .define('C', ingredient(Tags.Items.CHESTS_WOODEN))
                       .pattern(" c ")
                       .pattern("bCb")
                       .pattern("bBb")
                       .unlockedBy("has_item", has(TinkerTags.Items.GOLD_CASTS))
                       .save(consumer, recipeKey(prefix(TinkerTables.castChest, folder)));

    // modifier worktable
    ShapedRetexturedRecipeBuilder.fromShaped(
      shaped(RecipeCategory.DECORATIONS, TinkerTables.modifierWorktable)
        .define('r', ingredient(TinkerTags.Items.WORKSTATION_ROCK))
        .define('s', ingredient(TinkerTags.Items.SEARED_BLOCKS))
        .pattern("sss")
        .pattern("r r")
        .pattern("r r")
        .unlockedBy("has_item", has(TinkerTags.Items.SEARED_BLOCKS)))
      .setSource(TinkerIngredients.of(TinkerTags.Items.WORKSTATION_ROCK))
      .setMatchAll()
      .build(consumer, recipeKey(prefix(TinkerTables.modifierWorktable, folder)));

    // tinker anvil
    ShapedRetexturedRecipeBuilder.fromShaped(
      shaped(RecipeCategory.DECORATIONS, TinkerTables.tinkersAnvil)
        .define('m', ingredient(TinkerTags.Items.ANVIL_METAL))
        .define('s', ingredient(TinkerTags.Items.SEARED_BLOCKS))
        .pattern("mmm")
        .pattern(" s ")
        .pattern("sss")
        .unlockedBy("has_item", has(TinkerTags.Items.ANVIL_METAL)))
      .setSource(TinkerIngredients.of(TinkerTags.Items.ANVIL_METAL))
      .setMatchAll()
      .build(consumer, recipeKey(prefix(TinkerTables.tinkersAnvil, folder)));
    ShapedRetexturedRecipeBuilder.fromShaped(
      shaped(RecipeCategory.DECORATIONS, TinkerTables.scorchedAnvil)
        .define('m', ingredient(TinkerTags.Items.ANVIL_METAL))
        .define('s', ingredient(TinkerTags.Items.SCORCHED_BLOCKS))
        .pattern("mmm")
        .pattern(" s ")
        .pattern("sss")
        .unlockedBy("has_item", has(TinkerTags.Items.ANVIL_METAL)))
      .setSource(TinkerIngredients.of(TinkerTags.Items.ANVIL_METAL))
      .setMatchAll()
      .build(consumer, recipeKey(prefix(TinkerTables.scorchedAnvil, folder)));

    // tool forge - just a humor recipe
    Component toolForgeName = Component.translatable("block.tconstruct.tool_forge");
    ItemStack tinkersForgeStack = new ItemStack(TinkerTables.tinkersAnvil);
    tinkersForgeStack.set(DataComponents.CUSTOM_NAME, toolForgeName);
    ShapedRetexturedRecipeBuilder.fromShaped(
      shaped(RecipeCategory.DECORATIONS, ItemStackTemplate.fromNonEmptyStack(tinkersForgeStack))
        .define('m', ingredient(TinkerTags.Items.ANVIL_METAL))
        .define('s', ingredient(TinkerTags.Items.SEARED_BLOCKS))
        .define('t', TinkerTables.tinkerStation)
        .pattern("sss")
        .pattern("mtm")
        .pattern("m m")
        .unlockedBy("has_item", has(TinkerTags.Items.ANVIL_METAL)))
      .setSource(TinkerIngredients.of(TinkerTags.Items.ANVIL_METAL))
      .setMatchAll()
      .build(consumer, recipeKey(location(folder + "tinkers_forge")));
    ItemStack scorchedForgeStack = new ItemStack(TinkerTables.scorchedAnvil);
    scorchedForgeStack.set(DataComponents.CUSTOM_NAME, toolForgeName);
    ShapedRetexturedRecipeBuilder.fromShaped(
      shaped(RecipeCategory.DECORATIONS, ItemStackTemplate.fromNonEmptyStack(scorchedForgeStack))
        .define('m', ingredient(TinkerTags.Items.ANVIL_METAL))
        .define('s', ingredient(TinkerTags.Items.SCORCHED_BLOCKS))
        .define('t', TinkerTables.tinkerStation)
        .pattern("sss")
        .pattern("mtm")
        .pattern("m m")
        .unlockedBy("has_item", has(TinkerTags.Items.ANVIL_METAL)))
      .setSource(TinkerIngredients.of(TinkerTags.Items.ANVIL_METAL))
      .setMatchAll()
      .build(consumer, recipeKey(location(folder + "scorched_forge")));

    // material recipes - for the material fallbacks
    RecipeOutput materialConsumer = MaterialsConsumerBuilder.shaped("m").build(consumer);
    Ingredient fakeStorageBlock = MaterialTinkerIngredients.of(TinkerToolParts.fakeStorageBlock, MaterialPredicate.tag(TinkerTags.Materials.COMPATABILITY_ALLOYS));
    shaped(RecipeCategory.MISC, TinkerTables.tinkersAnvil)
      .define('m', fakeStorageBlock)
      .define('s', ingredient(TinkerTags.Items.SEARED_BLOCKS))
      .pattern("mmm")
      .pattern(" s ")
      .pattern("sss")
      .unlockedBy("has_item", has(TinkerToolParts.fakeStorageBlock))
      .save(materialConsumer, recipeKey(wrap(TinkerTables.tinkersAnvil, folder, "_material")));
    shaped(RecipeCategory.MISC, TinkerTables.scorchedAnvil)
      .define('m', fakeStorageBlock)
      .define('s', ingredient(TinkerTags.Items.SCORCHED_BLOCKS))
      .pattern("mmm")
      .pattern(" s ")
      .pattern("sss")
      .unlockedBy("has_item", has(TinkerToolParts.fakeStorageBlock))
      .save(materialConsumer, recipeKey(wrap(TinkerTables.scorchedAnvil, folder, "_material")));
    materialConsumer = MaterialsConsumerBuilder.shaped("m").build(consumer);
    shaped(RecipeCategory.DECORATIONS, ItemStackTemplate.fromNonEmptyStack(tinkersForgeStack))
      .define('m', fakeStorageBlock)
      .define('s', ingredient(TinkerTags.Items.SEARED_BLOCKS))
      .define('t', TinkerTables.tinkerStation)
      .pattern("sss")
      .pattern("mtm")
      .pattern("m m")
      .unlockedBy("has_item", has(TinkerToolParts.fakeStorageBlock))
      .save(materialConsumer, recipeKey(location(folder + "seared_forge_material")));
    shaped(RecipeCategory.DECORATIONS, ItemStackTemplate.fromNonEmptyStack(scorchedForgeStack))
      .define('m', fakeStorageBlock)
      .define('s', ingredient(TinkerTags.Items.SCORCHED_BLOCKS))
      .define('t', TinkerTables.tinkerStation)
      .pattern("sss")
      .pattern("mtm")
      .pattern("m m")
      .unlockedBy("has_item", has(TinkerToolParts.fakeStorageBlock))
      .save(materialConsumer, recipeKey(location(folder + "scorched_forge_material")));

    // part swapping
    TinkerStationPartSwappingBuilder.tools(DifferenceIngredient.of(TinkerIngredients.of(TinkerTags.Items.MULTIPART_TOOL), TinkerIngredients.of(TinkerTags.Items.UNSWAPPABLE_PARTS)))
      .save(consumer, location(folder + "tinker_station_part_swapping"));
    TinkerStationPartSwappingBuilder.tools(DifferenceIngredient.of(TinkerIngredients.of(TinkerTags.Items.MULTIPART_TOOL), TinkerIngredients.of(TinkerTags.Items.UNSWAPPABLE_TOOLS)))
      .fromTool().save(consumer, location(folder + "tool_material_swapping"));
    TinkerStationPartSwappingBuilder.tools(TinkerIngredients.of(TinkerTools.arrow.get(), TinkerTools.shuriken.get()))
      .maxStackSize(4)
      .save(consumer, location(folder + "ammo_part_swapping"));
    TinkerStationPartSwappingBuilder.tools(TinkerIngredients.of(TinkerTools.throwingAxe.get()))
      .maxStackSize(2)
      .save(consumer, location(folder + "throwing_axe_part_swapping"));

    // tool repair recipe
    SimpleRecipeOutput.save(consumer, location(folder + "tinker_station_repair"), TinkerTables.tinkerStationRepairSerializer.get());
    SimpleRecipeOutput.save(consumer, location(folder + "crafting_table_repair"), TinkerTables.craftingTableRepairSerializer.get());
  }

  private void damageRecipes(RecipeOutput consumer) {
    // tool damaging
    String damageFolder = "tables/tinker_station_damaging/";
    TinkerStationDamagingRecipeBuilder.damage(TinkerIngredients.of(TinkerFluids.magmaBottle), 20)
      .save(consumer, location(damageFolder + "magma_bottle"));
    TinkerStationDamagingRecipeBuilder.damage(TinkerIngredients.of(TinkerFluids.magma), 100)
      .save(consumer, location(damageFolder + "magma_bucket"));
    TinkerStationDamagingRecipeBuilder.damage(TinkerIngredients.of(TinkerFluids.venomBottle), 200)
      .save(consumer, location(damageFolder + "venom_bottle"));
    TinkerStationDamagingRecipeBuilder.damage(TinkerIngredients.of(TinkerFluids.venom), 1000)
      .save(consumer, location(damageFolder + "venom_bucket"));
    TinkerStationDamagingRecipeBuilder.damage(TinkerIngredients.of(Items.LAVA_BUCKET), 500)
      .save(consumer, location(damageFolder + "lava_bucket"));
    TinkerStationDamagingRecipeBuilder.damage(TinkerIngredients.of(TinkerFluids.blazingBlood), 2500)
      .save(consumer, location(damageFolder + "blazing_bucket"));
  }

  @SuppressWarnings("removal")
  private void recyclingRecipes(RecipeOutput consumer) {
    // recipes for recycling vanilla tools
    String folder = "tables/recycling/";

    // default tools, though skip anything that contains metal
    // wood
    Pattern rod = new Pattern(TConstruct.MOD_ID, "rod");
    PartBuilderRecycleBuilder.tool(Items.WOODEN_PICKAXE, Items.WOODEN_AXE)
      .result(rod, Items.STICK, 8)
      .save(consumer, location(folder + "wooden_axe"));
    PartBuilderRecycleBuilder.tool(Items.WOODEN_SWORD, Items.WOODEN_HOE)
      .result(rod, Items.STICK, 5)
      .save(consumer, location(folder + "wooden_sword"));
    PartBuilderRecycleBuilder.tool(Items.WOODEN_SHOVEL)
      .result(rod, Items.STICK, 4)
      .save(consumer, location(folder + "wooden_shovel"));
    Pattern string = new Pattern(TConstruct.MOD_ID, "bowstring");
    PartBuilderRecycleBuilder.tool(Items.BOW)
      .result(rod, Items.STICK, 3)
      .result(string, Items.STRING, 3)
      .save(consumer, location(folder + "bow"));
    Pattern ingot = new Pattern(TConstruct.MOD_ID, "ingot");
    PartBuilderRecycleBuilder.tool(Items.CROSSBOW)
      .result(rod, Items.STICK, 3)
      .result(string, Items.STRING, 2)
      .result(ingot, Tags.Items.INGOTS_IRON, 1)
      .save(consumer, location(folder + "crossbow"));
    PartBuilderRecycleBuilder.tool(Items.FISHING_ROD)
      .result(rod, Items.STICK, 3)
      .result(string, Items.STRING, 2)
      .save(consumer, location(folder + "fishing_rod"));
    // stone
    Pattern block = new Pattern(TConstruct.MOD_ID, "block");
    PartBuilderRecycleBuilder.tool(Items.STONE_PICKAXE, Items.STONE_AXE)
      .result(block, Items.COBBLESTONE, 3)
      .save(consumer, location(folder + "stone_axe"));
    PartBuilderRecycleBuilder.tool(Items.STONE_SWORD, Items.STONE_HOE)
      .result(block, Items.COBBLESTONE, 2)
      .save(consumer, location(folder + "stone_sword"));
    PartBuilderRecycleBuilder.tool(Items.STONE_SHOVEL)
      .result(block, Items.COBBLESTONE, 1)
      .save(consumer, location(folder + "stone_shovel"));
    // while you can melt it, flint and steel is literally just two items with nothing connecting them, so let the part builder recycle them
    PartBuilderRecycleBuilder.tool(Items.FLINT_AND_STEEL)
      .result(new Pattern(TConstruct.MOD_ID, "shard"), Items.FLINT, 1)
      .result(ingot, Items.IRON_INGOT, 1)
      .save(consumer, location(folder + "flint_and_steel"));

    // leather armor
    Pattern leather = new Pattern(TConstruct.MOD_ID, "maille");
    PartBuilderRecycleBuilder.tool(Items.LEATHER_HELMET)
      .result(leather, Items.LEATHER, 5)
      .save(consumer, location(folder + "leather_helmet"));
    PartBuilderRecycleBuilder.tool(Items.LEATHER_CHESTPLATE)
      .result(leather, Items.LEATHER, 8)
      .save(consumer, location(folder + "leather_chestplate"));
    PartBuilderRecycleBuilder.tool(Items.LEATHER_LEGGINGS, Items.LEATHER_HORSE_ARMOR)
      .result(leather, Items.LEATHER, 7)
      .save(consumer, location(folder + "leather_leggings"));
    PartBuilderRecycleBuilder.tool(Items.LEATHER_BOOTS)
      .result(leather, Items.LEATHER, 4)
      .save(consumer, location(folder + "leather_boots"));

    // turtle shell
    Pattern scale = new Pattern(TConstruct.MOD_ID, "scale");
    PartBuilderRecycleBuilder.tool(Items.TURTLE_HELMET)
      .result(scale, Items.TURTLE_SCUTE, 5)
      .save(consumer, location(folder + "turtle_helmet"));

    // twilight forest
    String tfId = "twilightforest";
    Function<String,Identifier> tf = name -> Identifier.fromNamespaceAndPath(tfId, name);
    RecipeOutput tfConsumer = withCondition(consumer, new ModLoadedCondition(tfId));
    // naga scale armor
    Identifier nagaScale = tf.apply("naga_scale");
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("naga_chestplate")))
      .result(scale, ItemNameOutput.fromName(nagaScale, 8))
      .save(tfConsumer, location(folder + "twilightforest/naga_chestplate"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("naga_leggings")))
      .result(scale, ItemNameOutput.fromName(nagaScale, 7))
      .save(tfConsumer, location(folder + "twilightforest/naga_leggings"));
    // ironwood armor and tools
    TagKey<Item> ironwoodIngot = ItemTags.create(Mantle.commonResource("ingots/ironwood"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("ironwood_pickaxe"), tf.apply("ironwood_axe")))
      .result(ingot, ironwoodIngot, 3)
      .save(tfConsumer, location(folder + "twilightforest/ironwood_axe"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("ironwood_sword"), tf.apply("ironwood_hoe")))
      .result(ingot, ironwoodIngot, 2)
      .save(tfConsumer, location(folder + "twilightforest/ironwood_sword"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("ironwood_shovel")))
      .result(ingot, ironwoodIngot, 1)
      .save(tfConsumer, location(folder + "twilightforest/ironwood_shovel"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("ironwood_helmet")))
      .result(ingot, ironwoodIngot, 5)
      .save(tfConsumer, location(folder + "twilightforest/ironwood_helmet"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("ironwood_chestplate")))
      .result(ingot, ironwoodIngot, 8)
      .save(tfConsumer, location(folder + "twilightforest/ironwood_chestplate"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("ironwood_leggings")))
      .result(ingot, ironwoodIngot, 7)
      .save(tfConsumer, location(folder + "twilightforest/ironwood_leggings"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("ironwood_boots")))
      .result(ingot, ironwoodIngot, 4)
      .save(tfConsumer, location(folder + "twilightforest/ironwood_boots"));
    // arctic
    Identifier arcticFur = tf.apply("arctic_fur");
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("arctic_helmet")))
      .result(leather, ItemNameOutput.fromName(arcticFur, 5))
      .save(tfConsumer, location(folder + "twilightforest/arctic_helmet"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("arctic_chestplate")))
      .result(leather, ItemNameOutput.fromName(arcticFur, 8))
      .save(tfConsumer, location(folder + "twilightforest/arctic_chestplate"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("arctic_leggings")))
      .result(leather, ItemNameOutput.fromName(arcticFur, 7))
      .save(tfConsumer, location(folder + "twilightforest/arctic_leggings"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("arctic_boots")))
      .result(leather, ItemNameOutput.fromName(arcticFur, 4))
      .save(tfConsumer, location(folder + "twilightforest/arctic_boots"));
    // arctic
    Identifier alphaYetiFur = tf.apply("alpha_yeti_fur");
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("yeti_helmet")))
      .result(leather, ItemNameOutput.fromName(alphaYetiFur, 5))
      .save(tfConsumer, location(folder + "twilightforest/yeti_helmet"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("yeti_chestplate")))
      .result(leather, ItemNameOutput.fromName(alphaYetiFur, 8))
      .save(tfConsumer, location(folder + "twilightforest/yeti_chestplate"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("yeti_leggings")))
      .result(leather, ItemNameOutput.fromName(alphaYetiFur, 7))
      .save(tfConsumer, location(folder + "twilightforest/yeti_leggings"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("yeti_boots")))
      .result(leather, ItemNameOutput.fromName(alphaYetiFur, 4))
      .save(tfConsumer, location(folder + "twilightforest/yeti_boots"));
  }
}
