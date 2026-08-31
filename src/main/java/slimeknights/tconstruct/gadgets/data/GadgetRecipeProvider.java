package slimeknights.tconstruct.gadgets.data;

import slimeknights.tconstruct.library.recipe.TinkerIngredients;

import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.Criterion;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.data.BaseRecipeProvider;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.gadgets.TinkerGadgets;
import slimeknights.tconstruct.gadgets.entity.FancyArmorStandEntity.StandType;
import slimeknights.tconstruct.gadgets.entity.FrameType;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.casting.ItemCastingRecipeBuilder;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.shared.TinkerMaterials;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.world.TinkerWorld;
import slimeknights.tconstruct.world.block.FoliageType;

import java.util.function.Consumer;

public class GadgetRecipeProvider extends BaseRecipeProvider {
  public GadgetRecipeProvider(net.minecraft.core.HolderLookup.Provider registries, RecipeOutput output) {
    super(registries, output);
  }

  @Override
  protected void buildTinkersRecipes(RecipeOutput consumer) {
    // piggybackpack
    String folder = "gadgets/";
    ItemCastingRecipeBuilder.tableRecipe(TinkerGadgets.piggyBackpack)
                            .setCast(Items.SADDLE, true)
                            .setFluidAndTime(TinkerFluids.skySlime, FluidValues.SLIMEBALL * 4)
                            .save(consumer, prefix(TinkerGadgets.piggyBackpack, folder));
    shaped(RecipeCategory.DECORATIONS, TinkerGadgets.punji)
                       .define('b', Items.BAMBOO)
                       .pattern(" b ")
                       .pattern("bbb")
                       .unlockedBy("has_item", has(Items.BAMBOO))
                       .save(consumer, recipeKey(prefix(TinkerGadgets.punji, folder)));

    // frames
    folder = "gadgets/fancy_frame/";
    frameCrafting(consumer, Tags.Items.NUGGETS_GOLD, FrameType.GOLD);
    frameCrafting(consumer, TinkerMaterials.manyullyn.getNuggetTag(), FrameType.MANYULLYN);
    frameCrafting(consumer, TinkerTags.Items.NUGGETS_NETHERITE, FrameType.NETHERITE);
    shaped(RecipeCategory.DECORATIONS, TinkerGadgets.itemFrame.get(FrameType.DIAMOND))
                       .define('e', TinkerCommons.obsidianPane)
                       .define('M', ingredient(Tags.Items.GEMS_DIAMOND))
                       .pattern(" e ")
                       .pattern("eMe")
                       .pattern(" e ")
                       .unlockedBy("has_item", has(Tags.Items.GEMS_DIAMOND))
                       .group(prefix("fancy_item_frame"))
                       .save(consumer, recipeKey(location("gadgets/frame/" + FrameType.DIAMOND.getSerializedName())));
    shaped(RecipeCategory.DECORATIONS, TinkerGadgets.itemFrame.get(FrameType.CLEAR))
                       .define('e', ingredient(Tags.Items.GLASS_PANES_COLORLESS))
                       .define('M', ingredient(Tags.Items.GLASS_BLOCKS_COLORLESS))
                       .pattern(" e ")
                       .pattern("eMe")
                       .pattern(" e ")
                       .unlockedBy("has_item", has(Tags.Items.GLASS_PANES_COLORLESS))
                       .group(prefix("fancy_item_frame"))
                       .save(consumer, recipeKey(location(folder + FrameType.CLEAR.getSerializedName())));
    Item goldFrame = TinkerGadgets.itemFrame.get(FrameType.GOLD);
    Item reversedFrame = TinkerGadgets.itemFrame.get(FrameType.REVERSED_GOLD);
    shapeless(RecipeCategory.DECORATIONS, reversedFrame)
                          .requires(goldFrame)
                          .requires(Items.REDSTONE_TORCH)
                          .unlockedBy("has_item", has(goldFrame))
                          .group(prefix("reverse_fancy_item_frame"))
                          .save(consumer, recipeKey(location(folder + FrameType.REVERSED_GOLD.getSerializedName())));
    shapeless(RecipeCategory.DECORATIONS, goldFrame)
                          .requires(reversedFrame)
                          .requires(Items.REDSTONE_TORCH)
                          .unlockedBy("has_item", has(reversedFrame))
                          .group(prefix("reverse_fancy_item_frame"))
                          .save(consumer, recipeKey(location(folder + "reversed_reversed_gold")));

    String cakeFolder = "gadgets/cake/";
    TinkerGadgets.cake.forEach((foliage, cake) -> {
      if (foliage != FoliageType.ICHOR) {
        SlimeType slime = foliage.asSlime();
        ItemLike grass = TinkerWorld.slimeTallGrass.get(foliage);
        shaped(RecipeCategory.FOOD, cake)
                           .define('M', slime != null ? TinkerFluids.slime.get(slime).getBucket() : TinkerFluids.honey.asItem())
                           .define('S', foliage.isNether()
                             ? TinkerIngredients.of(Tags.Items.DUSTS_GLOWSTONE)
                             : foliage == FoliageType.ENDER ? TinkerIngredients.of(Tags.Items.DUSTS_REDSTONE) : TinkerIngredients.of(Items.SUGAR))
                           .define('E', Items.EGG)
                           .define('W', TinkerWorld.slimeTallGrass.get(foliage))
                           .pattern("MMM").pattern("SES").pattern("WWW")
                           .unlockedBy("has_slime", has(grass))
                           .save(consumer, recipeKey(location(cakeFolder + foliage.getSerializedName())));
      }
    });
    shaped(RecipeCategory.FOOD, TinkerGadgets.cake.get(FoliageType.ICHOR))
      .define('M', TinkerFluids.ichor)
      .define('S', TinkerIngredients.of(Tags.Items.DUSTS_GLOWSTONE))
      .define('E', Items.EGG)
      .define('W', Blocks.WARPED_ROOTS) // TODO: switch to ichor foliage one day
      .pattern("WWW").pattern("SES").pattern("MMM")
      .unlockedBy("has_slime", has(TinkerFluids.ichor))
      .save(consumer, recipeKey(location(cakeFolder + "ichor")));
    Item bucket = TinkerFluids.magma.asItem();
    shaped(RecipeCategory.FOOD, TinkerGadgets.magmaCake)
                       .define('M', bucket)
                       .define('S', TinkerIngredients.of(Tags.Items.DUSTS_GLOWSTONE))
                       .define('E', Items.EGG)
                       .define('W', Blocks.CRIMSON_ROOTS)
                       .pattern("MMM").pattern("SES").pattern("WWW")
                       .unlockedBy("has_slime", has(bucket))
                       .save(consumer, recipeKey(location(cakeFolder + "magma")));

    // armor stands
    String standFolder = "gadgets/armor_stands/";
    shaped(RecipeCategory.DECORATIONS, TinkerGadgets.armorStand.get(StandType.BAMBOO))
      .define('S', Items.BAMBOO)
      .define('B', TinkerTables.pattern)
      .pattern("SSS").pattern(" S ").pattern("SBS")
      .unlockedBy("has_base", has(TinkerTables.pattern))
      .save(consumer, recipeKey(location(standFolder + "bamboo")));
    shaped(RecipeCategory.DECORATIONS, TinkerGadgets.armorStand.get(StandType.BONE))
      .define('S', TinkerIngredients.of(TinkerTags.Items.BONES))
      .define('B', TinkerSmeltery.searedPaver.getSlab())
      .pattern("SSS").pattern(" S ").pattern("SBS")
      .unlockedBy("has_base", has(TinkerSmeltery.searedPaver.getSlab()))
      .save(consumer, recipeKey(location(standFolder + "bone")));
    shaped(RecipeCategory.DECORATIONS, TinkerGadgets.armorStand.get(StandType.NECROTIC_BONE))
      .define('S', TinkerIngredients.of(TinkerTags.Items.WITHER_BONES))
      .define('B', TinkerMaterials.blazewood.getSlab())
      .pattern("SSS").pattern(" S ").pattern("SBS")
      .unlockedBy("has_base", has(TinkerMaterials.blazewood.getSlab()))
      .save(consumer, recipeKey(location(standFolder + "bloodshroom")));
    shaped(RecipeCategory.DECORATIONS, TinkerGadgets.armorStand.get(StandType.CLEAR))
      .define('S', TinkerIngredients.of(Tags.Items.GLASS_PANES_COLORLESS))
      .define('B', TinkerIngredients.of(Tags.Items.GLASS_BLOCKS_COLORLESS))
      .pattern("SSS").pattern(" S ").pattern("SBS")
      .unlockedBy("has_base", has(Tags.Items.GLASS_BLOCKS_COLORLESS))
      .save(consumer, recipeKey(location(standFolder + "clear")));
  }


  /* Helpers */

  /**
   * Adds a recipe to the campfire, furnace, and smoker
   * @param consumer    Recipe consumer
   * @param input       Recipe input
   * @param output      Recipe output
   * @param experience  Experience for the recipe
   * @param folder      Folder to store the recipe
   */
  private void foodCooking(RecipeOutput consumer, ItemLike input, ItemLike output, float experience, String folder) {
    SimpleCookingRecipeBuilder.campfireCooking(TinkerIngredients.of(input), RecipeCategory.FOOD, output, experience, 600)
                              .unlockedBy("has_item", has(input))
                              .save(consumer, recipeKey(wrap(id(output), folder, "_campfire")));
    // furnace is 200 ticks
    Identifier outputId = id(output);
    Criterion<InventoryChangeTrigger.TriggerInstance> criteria = has(input);
    SimpleCookingRecipeBuilder.smelting(TinkerIngredients.of(input), RecipeCategory.FOOD, CookingBookCategory.FOOD, output, experience, 200)
                              .unlockedBy("has_item", criteria)
                              .save(consumer, recipeKey(wrap(outputId, folder, "_furnace")));
    // smoker 100 ticks
    SimpleCookingRecipeBuilder.smoking(TinkerIngredients.of(input), RecipeCategory.FOOD, output, experience, 100)
                              .unlockedBy("has_item", criteria)
                              .save(consumer, recipeKey(wrap(outputId, folder, "_smoker")));
  }

  /**
   * Adds a recipe for an item frame type
   * @param consumer  Recipe consumer
   * @param edges     Edge item
   * @param type      Frame type
   */
  private void frameCrafting(RecipeOutput consumer, TagKey<Item> edges, FrameType type) {
    shaped(RecipeCategory.DECORATIONS, TinkerGadgets.itemFrame.get(type))
                       .define('e', ingredient(edges))
                       .define('M', TinkerCommons.obsidianPane)
                       .pattern(" e ")
                       .pattern("eMe")
                       .pattern(" e ")
                       .unlockedBy("has_item", has(edges))
                       .group(prefix("fancy_item_frame"))
                       .save(consumer, recipeKey(location("gadgets/frame/" + type.getSerializedName())));
  }
}
