package slimeknights.tconstruct.library.recipe.fuel;

import slimeknights.tconstruct.library.recipe.FluidTinkerIngredients;
import slimeknights.tconstruct.library.recipe.TinkerIngredients;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator.DuelSidedListener;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Class handling a recipe cache for fuel recipes, since any given entity type has one recipe
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MeltingFuelLookup {
  /** Dummy fuel instance sine caches don't support caching null */
  private static final MeltingFuel EMPTY = new MeltingFuel(Identifier.parse("missingno"), FluidTinkerIngredients.EMPTY, 0, 0, 0);
  /** Temperature for solid fuels in the heater */
  private static MeltingFuel SOLID = EMPTY;
  /** List of all recipes */
  private static final List<MeltingFuel> RECIPES = new ArrayList<>();
  /** Mapping from fluid to fuel */
  private static final Map<Fluid,MeltingFuel> CACHE = new HashMap<>();
  /** Logic to fill the cache */
  private static final Function<Fluid,MeltingFuel> LOOKUP = fluid -> {
    for (MeltingFuel recipe : RECIPES) {
      if (recipe.matches(fluid)) {
        return recipe;
      }
    }
    return EMPTY;
  };
  /** Listener to check when recipes reload */
  private static final DuelSidedListener LISTENER = RecipeCacheInvalidator.addDuelSidedListener(() -> {
    SOLID = EMPTY;
    RECIPES.clear();
    CACHE.clear();
  });

  /**
   * Adds a melting fuel to the lookup
   * @param fuel   Fuel
   */
  public static void addFuel(MeltingFuel fuel) {
    // skip empty fuel
    if (fuel.getRate() == 0) {
      return;
    }
    LISTENER.checkClear();
    if (fuel.getInput() != FluidTinkerIngredients.EMPTY) {
      RECIPES.add(fuel);
    } else if (SOLID == EMPTY || SOLID.getId().equals(fuel.getId())) {
      // Integrated singleplayer decodes the same synced recipe once for the
      // server and once for the client in one JVM.  Treat that same-id replay
      // as the same recipe; only different solid-fuel IDs are a real conflict.
      SOLID = fuel;
    } else {
      TConstruct.LOG.warn("Multiple fuel recipes for solid fuel. This usually indicates a datapack error and may cause desyncs. Original {}, latest {}", SOLID.getId(), fuel.getId());
    }
  }

  /** Checks if the given fluid is a fuel */
  public static boolean isFuel(Fluid fluid) {
    return CACHE.computeIfAbsent(fluid, LOOKUP) != EMPTY;
  }

  /** Gets the properties for solid fuel */
  public static MeltingFuel getSolid() {
    return SOLID;
  }

  /**
   * Gets the recipe for the given fluid
   * @param fluid   Fluid found
   * @return  Recipe, or null if no recipe for this type
   */
  @Nullable
  public static MeltingFuel findFuel(Fluid fluid) {
    MeltingFuel recipe = CACHE.computeIfAbsent(fluid, LOOKUP);
    if (recipe == EMPTY) {
      return null;
    }
    return recipe;
  }
}
