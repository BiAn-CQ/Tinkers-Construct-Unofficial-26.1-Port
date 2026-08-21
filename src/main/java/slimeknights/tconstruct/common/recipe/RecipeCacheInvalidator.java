package slimeknights.tconstruct.common.recipe;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.resource.VanillaServerListeners;
import slimeknights.mantle.data.listener.IEarlySafeManagerReloadListener;
import slimeknights.tconstruct.TConstruct;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that handles notifying recipe caches that they need to invalidate
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RecipeCacheInvalidator implements IEarlySafeManagerReloadListener {
  private static final RecipeCacheInvalidator INSTANCE = new RecipeCacheInvalidator();
  private static final List<BooleanConsumer> listeners = new ArrayList<>();

  /**
   * Adds a new listener that runs every time the recipes are reloaded
   * @param runnable  Runnable accepting a boolean representing if this is client side
   */
  public static void addReloadListener(BooleanConsumer runnable) {
    listeners.add(runnable);
  }

  /**
   * Registers a listener that properly responds to the client side
   * @param runnable  Runnable to clear cache
   * @return  Object that can clear cache as needed
   */
  public static DuelSidedListener addDuelSidedListener(Runnable runnable) {
    DuelSidedListener listener = new DuelSidedListener(runnable);
    addReloadListener(listener);
    return listener;
  }

  /**
   * Reloads all listeners, used client side
   */
  public static void reload(boolean client) {
    for (BooleanConsumer runnable : listeners) {
      runnable.accept(client);
    }
  }

  @Override
  public void onReloadSafe(ResourceManager resourceManager) {
    reload(false);
  }

  /**
   * Called when resource managers reload
   * @param event  Reload event
   */
  public static void onReloadListenerReload(AddServerReloadListenersEvent event) {
    var key = TConstruct.getResource("recipe_cache");
    event.addListener(key, INSTANCE);
    // Material recipes populate their caches while RecipeManager prepares the recipe JSONs.
    // Clear the previous reload's values before that preparation starts; the default ordering
    // places mod listeners after recipes and would erase the newly populated material cache.
    event.addDependency(key, VanillaServerListeners.RECIPES);
  }

  /** Logic to respond properly to late running of the client */
  public static class DuelSidedListener implements BooleanConsumer {
    private final Runnable clearCache;
    private boolean clearQueued = false;

    private DuelSidedListener(Runnable clearCache) {
      this.clearCache = clearCache;
    }

    @Override
    public void accept(boolean client) {
      // client side event runs at the end of recipe loading
      // server side runs at the start
      // so queue client side to run at the beginning of the next recipe list
      if (client) {
        clearQueued = true;
      } else {
        clearCache();
      }
    }

    /**
     * Clears the cache based on the runnable
     */
    public void clearCache() {
      clearQueued = false;
      clearCache.run();
    }

    /**
     * Clears the cache if a clear is queued. Intended to be called during add
     */
    public void checkClear() {
      if (clearQueued) {
        clearCache();
      }
    }
  }
}
