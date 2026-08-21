package slimeknights.tconstruct.common.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.TConstruct;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecipeCacheInvalidatorTest {
  @Test
  void cacheInvalidatorRunsBeforeRecipeLoading() {
    RecipeManager recipeManager = new RecipeManager(RegistryAccess.EMPTY);
    ReloadableServerResources resources = mock(ReloadableServerResources.class);
    when(resources.listeners()).thenReturn(List.of(recipeManager));
    AddServerReloadListenersEvent event = new AddServerReloadListenersEvent(resources, RegistryAccess.EMPTY, new HashMap<>());

    RecipeCacheInvalidator.onReloadListenerReload(event);

    var invalidator = event.getRegistry().get(TConstruct.getResource("recipe_cache"));
    assertThat(invalidator).isNotNull();
    assertThat(event.getGraph().hasEdgeConnecting(invalidator, recipeManager)).isTrue();
  }
}
