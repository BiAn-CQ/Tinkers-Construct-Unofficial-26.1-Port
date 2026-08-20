package slimeknights.tconstruct.library.utils;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import slimeknights.mantle.recipe.IMultiRecipe;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Compatibility access for recipe data after 26.1 removed Level.getRecipeManager().
 * Server levels retain the authoritative manager; client-only callers receive
 * an empty manager backed by the current registry access until they migrate to
 * the 26.1 recipe-display/client recipe protocol.
 */
public final class TinkerRecipeHelper {
  /**
   * 26.1 keeps the full recipe map on the server and only synchronizes the
   * recipe property sets to a normal client.  NeoForge sends the requested
   * custom recipe values through {@code RecipesReceivedEvent}; keep that map
   * here as the client-side source for JEI and the book.
   */
  private static volatile RecipeMap clientRecipeMap = RecipeMap.EMPTY;

  private TinkerRecipeHelper() {}

  /** Stores the recipe subset received from the server on the client. */
  public static void setClientRecipeMap(RecipeMap recipeMap) {
    clientRecipeMap = recipeMap == null ? RecipeMap.EMPTY : recipeMap;
  }

  /** Clears client recipe data when leaving a world. */
  public static void clearClientRecipeMap() {
    clientRecipeMap = RecipeMap.EMPTY;
  }

  /**
   * Returns the authoritative values when available, falling back to the
   * synchronized client subset while running on the client.
   */
  private static Collection<RecipeHolder<?>> values(RecipeManager manager) {
    Collection<RecipeHolder<?>> values = manager.getRecipes();
    return values.isEmpty() ? clientRecipeMap.values() : values;
  }

  public static RecipeManager getRecipeManager(Level level) {
    if (level instanceof ServerLevel serverLevel) {
      return serverLevel.getServer().getRecipeManager();
    }
    if (level.getServer() != null) {
      return level.getServer().getRecipeManager();
    }
    return new RecipeManager(level.registryAccess());
  }

  @SuppressWarnings("unchecked")
  public static <T extends Recipe<?>> List<RecipeHolder<T>> getAllRecipesFor(RecipeManager manager, RecipeType<T> type) {
    return values(manager).stream()
      .filter(holder -> holder.value().getType() == type)
      .map(holder -> (RecipeHolder<T>) holder)
      .toList();
  }

  public static <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> getRecipesFor(RecipeManager manager, RecipeType<T> type, I input, Level level) {
    return values(manager).stream()
      .filter(holder -> holder.value().getType() == type)
      .filter(holder -> ((Recipe<I>) holder.value()).matches(input, level))
      .map(holder -> (RecipeHolder<T>) holder)
      .toList();
  }

  /**
   * Client book compatibility for Mantle's former UI recipe helper.
   *
   * <p>26.1 no longer ships Mantle's {@code RecipeHelper}; the book only
   * needs the matching recipe values, so filter the authoritative manager
   * directly and keep the old call site semantics.</p>
   */
  public static <I extends RecipeInput, T extends Recipe<I>, R extends T> List<R> getUIRecipes(
    RecipeManager manager, RecipeType<T> type, Class<R> recipeClass, Predicate<? super R> filter) {
    return values(manager).stream()
      .filter(holder -> holder.value().getType() == type)
      .map(holder -> holder.value())
      .filter(recipeClass::isInstance)
      .map(recipeClass::cast)
      .filter(filter)
      .toList();
  }

  /**
   * Client book compatibility for Mantle's former JEI recipe helper.
   * Registry access is retained in the signature for source compatibility;
   * recipe display pages only require the recipe manager in 26.1.
   */
  public static <R> List<R> getJEIRecipes(
    RegistryAccess access, RecipeManager manager, RecipeType<?> type, Class<R> recipeClass) {
    return values(manager).stream()
      .filter(holder -> holder.value().getType() == type)
      .map(holder -> holder.value())
      // Dynamic recipes store a compact server recipe but expose their full
      // ingredient/output variants only for display (materials, casts, parts,
      // modifiers, and similar recipes).
      .flatMap(recipe -> {
        if (recipe instanceof IMultiRecipe<?> multiRecipe) {
          return multiRecipe.getRecipes(access).stream();
        }
        return Stream.of(recipe);
      })
      .filter(recipeClass::isInstance)
      .map(recipeClass::cast)
      .toList();
  }

  /**
   * Compatibility access for the former Mantle recipe helper.  JEI's fuel
   * category only needs recipe values, so filtering the client recipe manager
   * is sufficient on 26.1.
   */
  public static <R> List<R> getRecipes(RecipeManager manager, RecipeType<?> type, Class<R> recipeClass) {
    return values(manager).stream()
      .filter(holder -> holder.value().getType() == type)
      .map(holder -> holder.value())
      .filter(recipeClass::isInstance)
      .map(recipeClass::cast)
      .toList();
  }
}
