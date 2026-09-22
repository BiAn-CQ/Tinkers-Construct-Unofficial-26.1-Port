package slimeknights.tconstruct.library.utils;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.recipe.display.VanillaFilteredRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.AbstractCraftingTinkeringRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.repairing.ModifierRepairCraftingRecipe;
import slimeknights.tconstruct.test.CoreTestBootstrap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class VanillaFilteredRecipeTest extends CoreTestBootstrap {
  @AfterEach
  void clearClientRecipes() {
    TinkerRecipeHelper.clearClientRecipeMap();
  }

  @Test
  void expandsCraftingRepairDisplaysFromSyncedClientRecipes() {
    RecipeManager manager = mock(RecipeManager.class);
    when(manager.getRecipes()).thenReturn(List.of());
    HolderLookup.Provider access = mock(HolderLookup.Provider.class);
    ModifierRepairCraftingRecipe recipe = mock(ModifierRepairCraftingRecipe.class);
    when(recipe.getType()).thenReturn(RecipeType.CRAFTING);
    AbstractCraftingTinkeringRecipe display = mock(AbstractCraftingTinkeringRecipe.class);
    when(recipe.getFilteredRecipes(access)).thenReturn(List.of(display));
    RecipeHolder<CraftingRecipe> holder = new RecipeHolder<>(ResourceKey.create(Registries.RECIPE,
      Identifier.fromNamespaceAndPath("tconstruct", "tools/modifiers/tasty_crafting_table")), recipe);
    TinkerRecipeHelper.setClientRecipeMap(RecipeMap.create(List.of(holder)));

    assertThat(VanillaFilteredRecipe.getRecipes(access, manager, RecipeType.CRAFTING,
      AbstractCraftingTinkeringRecipe.class)).containsExactly(display);
  }
}
