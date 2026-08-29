package slimeknights.tconstruct.library.utils;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TinkerRecipeHelperTest {
  @AfterEach
  void clearClientRecipes() {
    TinkerRecipeHelper.clearClientRecipeMap();
  }

  @SuppressWarnings("unchecked")
  @Test
  void getRecipeForUsesSynchronizedClientRecipeMapWhenVanillaManagerIsEmpty() {
    RecipeManager manager = mock(RecipeManager.class);
    when(manager.getRecipes()).thenReturn(List.of());
    RecipeType<Recipe<RecipeInput>> type = mock(RecipeType.class);
    RecipeInput input = mock(RecipeInput.class);
    Level level = mock(Level.class);
    Recipe<RecipeInput> recipe = mock(Recipe.class);
    doReturn(type).when(recipe).getType();
    when(recipe.matches(input, level)).thenReturn(true);

    ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("tconstruct", "client_test"));
    RecipeHolder<Recipe<RecipeInput>> holder = new RecipeHolder<>(key, recipe);
    TinkerRecipeHelper.setClientRecipeMap(RecipeMap.create(List.of(holder)));

    assertThat(TinkerRecipeHelper.getRecipeFor(manager, type, input, level)).contains(holder);
  }
}
