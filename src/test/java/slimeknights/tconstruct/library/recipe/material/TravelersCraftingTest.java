package slimeknights.tconstruct.library.recipe.material;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.materials.IMaterialRegistry;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.Material;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.recipe.casting.material.MaterialCastingLookup;
import slimeknights.tconstruct.library.recipe.casting.material.MaterialFluidRecipe;
import slimeknights.tconstruct.library.tools.definition.ToolDefinitionData;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.test.CoreTestBootstrap;
import slimeknights.tconstruct.tools.stats.PlatingMaterialStats;
import slimeknights.tconstruct.tools.stats.StatlessMaterialStats;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TravelersCraftingTest extends CoreTestBootstrap {
  private static JsonObject resource(String path) throws Exception {
    try (var reader = new InputStreamReader(TravelersCraftingTest.class.getResourceAsStream("/data/tconstruct/" + path + ".json"), StandardCharsets.UTF_8)) {
      return JsonParser.parseReader(reader).getAsJsonObject();
    }
  }

  @Test
  void dataGeneratorPreservesRequestedPartOrderAndRepeatedParts() {
    var lining = Ingredient.of(Items.LEATHER);
    var plating = Ingredient.of(Items.IRON_INGOT);
    var pattern = ShapedRecipePattern.of(Map.of('l', lining, 'c', plating), "l l", "c c");
    var recipe = new ShapedRecipe(RecipeBuilder.createCraftingCommonInfo(true),
      RecipeBuilder.createCraftingBookInfo(RecipeCategory.COMBAT, null), pattern, new ItemStackTemplate(Items.IRON_HELMET));
    RecipeOutput output = mock(RecipeOutput.class);
    var id = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("test", "ordered_materials"));
    MaterialsConsumerBuilder.shaped("clc").build(output).accept(id, recipe, null);
    var result = ArgumentCaptor.forClass(net.minecraft.world.item.crafting.Recipe.class);
    verify(output).accept(eq(id), result.capture(), isNull(), any(net.neoforged.neoforge.common.conditions.ICondition[].class));
    var converted = (ShapedMaterialsRecipe) result.getValue();
    assertThat(converted.getParts()).hasSize(3);
    assertThat(converted.getParts().get(0).test(new ItemStack(Items.IRON_INGOT))).isTrue();
    assertThat(converted.getParts().get(1).test(new ItemStack(Items.LEATHER))).isTrue();
    assertThat(converted.getParts().get(2)).isSameAs(converted.getParts().get(0));
    assertThat(converted.pattern.ingredients().get(3).orElseThrow()).isSameAs(converted.getParts().get(0));
  }

  @Test
  void craftedArmorUsesPlatingStatsForDifferentMetalsAndLinings() throws Exception {
    var registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    var ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
    IMaterialRegistry registry = mock(IMaterialRegistry.class, CALLS_REAL_METHODS);
    when(registry.getMaterial(any())).thenAnswer(invocation -> new Material(((MaterialId)invocation.getArgument(0)).location(), 1, 0, Rarity.COMMON, false, false));
    Map<MaterialId, Map<slimeknights.tconstruct.library.materials.stats.MaterialStatsId, IMaterialStats>> stats = new HashMap<>();
    for (String metal : List.of("copper", "iron")) {
      var data = resource("tinkering/materials/stats/" + metal).getAsJsonObject("stats");
      Map<slimeknights.tconstruct.library.materials.stats.MaterialStatsId, IMaterialStats> materialStats = new HashMap<>();
      for (var type : List.of(PlatingMaterialStats.HELMET, PlatingMaterialStats.CHESTPLATE, PlatingMaterialStats.LEGGINGS, PlatingMaterialStats.BOOTS)) {
        var entry = data.getAsJsonObject(type.getId().toString());
        materialStats.put(type.getId(), new PlatingMaterialStats(type, entry.get("durability").getAsInt(), entry.get("armor").getAsFloat(), 0, 0));
      }
      stats.put(new MaterialId("tconstruct", metal), materialStats);
    }
    for (String lining : List.of("leather", "wool")) {
      stats.put(new MaterialId("tconstruct", lining), Map.of(StatlessMaterialStats.CUIRASS.getIdentifier(), StatlessMaterialStats.CUIRASS));
    }
    when(registry.getMaterialStats(any(), any())).thenAnswer(invocation -> Optional.ofNullable(stats.getOrDefault(invocation.getArgument(0), Map.of()).get(invocation.getArgument(1))));
    var loadedField = ModifierManager.class.getDeclaredField("dynamicModifiersLoaded");
    loadedField.setAccessible(true);
    boolean oldLoaded = loadedField.getBoolean(ModifierManager.INSTANCE);
    try (var materials = mockStatic(MaterialRegistry.class); var tags = mockStatic(TinkerTags.class);
         var cache = mockStatic(MaterialRecipeCache.class); var casting = mockStatic(MaterialCastingLookup.class)) {
      materials.when(MaterialRegistry::getInstance).thenReturn(registry);
      materials.when(() -> MaterialRegistry.getMaterial(any())).thenAnswer(invocation -> registry.getMaterial(invocation.getArgument(0)));
      materials.when(MaterialRegistry::isFullyLoaded).thenReturn(true);
      tags.when(TinkerTags::isTagsLoaded).thenReturn(true);
      casting.when(() -> MaterialCastingLookup.getCastingFluids(any())).thenReturn(List.of(mock(MaterialFluidRecipe.class)));
      loadedField.setBoolean(ModifierManager.INSTANCE, true);
      for (String name : List.of("goggles", "chestplate", "pants", "boots")) {
        var json = resource("recipe/tools/armor/travelers/" + name);
        var recipe = ShapedMaterialsRecipe.SERIALIZER.codec().codec().parse(ops, json).getOrThrow();
        Item item = recipe.assemble(CraftingInput.EMPTY).getItem();
        var definition = ((IModifiable)item).getToolDefinition();
        var oldDefinition = definition.getData();
        String definitionName = BuiltInRegistries.ITEM.getKey(item).getPath();
        definition.setData(ToolDefinitionData.LOADABLE.deserialize(resource("tinkering/tool_definitions/" + definitionName)));
        try {
          for (String metal : List.of("copper", "iron")) {
            var metalId = new MaterialId("tconstruct", metal);
            Item metalItem = metal.equals("copper") ? Items.COPPER_INGOT : Items.IRON_INGOT;
            for (String lining : List.of("leather", "wool#pink")) {
              MaterialVariantId liningId = MaterialVariantId.parse("tconstruct:" + lining);
              Item liningItem = lining.equals("leather") ? Items.LEATHER : Items.PINK_WOOL;
              cache.when(() -> MaterialRecipeCache.findRecipe(any())).thenAnswer(invocation -> {
                Item input = ((ItemStack)invocation.getArgument(0)).getItem();
                if (input != metalItem && input != liningItem) return MaterialRecipe.EMPTY;
                MaterialRecipe materialRecipe = mock(MaterialRecipe.class);
                when(materialRecipe.getValue()).thenReturn(1);
                when(materialRecipe.getNeeded()).thenReturn(1);
                when(materialRecipe.getMaterial()).thenReturn(MaterialVariant.of(input == metalItem ? metalId : liningId));
                return materialRecipe;
              });
              List<ItemStack> inputs = new ArrayList<>();
              for (var row : json.getAsJsonArray("pattern")) {
                for (char symbol : row.getAsString().toCharArray()) {
                  if (symbol == ' ') inputs.add(ItemStack.EMPTY);
                  else {
                    String ingredient = json.getAsJsonObject("key").get(String.valueOf(symbol)).toString();
                    inputs.add(new ItemStack(ingredient.contains("plating_") ? metalItem : ingredient.contains("cuirass") ? liningItem : Items.STRING));
                  }
                }
              }
              var crafted = recipe.assemble(CraftingInput.of(recipe.getWidth(), recipe.getHeight(), inputs));
              var tool = ToolStack.from(crafted);
              String context = name + " / " + metal + " / " + lining;
              assertThat(tool.getMaterials().get(0).getVariant()).as(context + " plating").isEqualTo(metalId);
              assertThat(tool.getMaterials().get(1).getVariant()).as(context + " lining").isEqualTo(liningId);
              float expected = stats.get(metalId).values().stream().map(PlatingMaterialStats.class::cast)
                .filter(stat -> stat.getIdentifier().toString().endsWith(definitionName.substring("travelers_".length())))
                .findFirst().orElseThrow().durability() * 0.75f * 0.75f;
              assertThat(tool.getStats().get(ToolStats.DURABILITY)).as(context + " durability").isEqualTo(expected);
            }
          }
        } finally {
          definition.setData(oldDefinition);
        }
      }
    } finally {
      loadedField.setBoolean(ModifierManager.INSTANCE, oldLoaded);
    }
  }
}
