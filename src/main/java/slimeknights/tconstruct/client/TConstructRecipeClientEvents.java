package slimeknights.tconstruct.client;

import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator;
import slimeknights.tconstruct.library.utils.TinkerRecipeHelper;

/** Receives the custom recipe subset requested from the server on 26.1. */
@EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT)
public final class TConstructRecipeClientEvents {
  private TConstructRecipeClientEvents() {}

  @SubscribeEvent
  static void recipesReceived(RecipesReceivedEvent event) {
    RecipeMap recipeMap = event.getRecipeMap();
    TinkerRecipeHelper.setClientRecipeMap(recipeMap);
    RecipeCacheInvalidator.reload(true);
    TConstruct.LOG.info("Received {} synchronized TConstruct recipes across {} recipe types",
      recipeMap.values().size(), event.getRecipeTypes().size());
  }

  @SubscribeEvent
  static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
    TinkerRecipeHelper.clearClientRecipeMap();
    DynamicTableParticleExtensions.clearCache();
  }
}
