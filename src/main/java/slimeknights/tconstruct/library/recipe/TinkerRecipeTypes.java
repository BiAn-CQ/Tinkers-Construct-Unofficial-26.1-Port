package slimeknights.tconstruct.library.recipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe;
import slimeknights.tconstruct.library.recipe.casting.ICastingRecipe;
import slimeknights.tconstruct.library.recipe.entitymelting.EntityMeltingRecipe;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipe;
import slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.severing.SeveringRecipe;
import slimeknights.tconstruct.library.recipe.molding.MoldingRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.IPartBuilderRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.library.recipe.worktable.IModifierWorktableRecipe;

import java.util.List;

/**
 * Class containing all of Tinkers Construct recipe types
 */
public class TinkerRecipeTypes {
  /** Deferred instance */
  private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, TConstruct.MOD_ID);

  public static final DeferredHolder<RecipeType<?>, ? extends RecipeType<IPartBuilderRecipe>> PART_BUILDER = register("part_builder");
  public static final DeferredHolder<RecipeType<?>, ? extends RecipeType<MaterialRecipe>> MATERIAL = register("material");
  public static final DeferredHolder<RecipeType<?>, ? extends RecipeType<ITinkerStationRecipe>> TINKER_STATION = register("tinker_station");
  public static final DeferredHolder<RecipeType<?>, ? extends RecipeType<IModifierWorktableRecipe>> MODIFIER_WORKTABLE = register("modifier_worktable");

  // casting
  public static final DeferredHolder<RecipeType<?>, ? extends RecipeType<ICastingRecipe>> CASTING_BASIN = register("casting_basin");
  public static final DeferredHolder<RecipeType<?>, ? extends RecipeType<ICastingRecipe>> CASTING_TABLE = register("casting_table");
  public static final DeferredHolder<RecipeType<?>, ? extends RecipeType<MoldingRecipe>> MOLDING_TABLE = register("molding_table");
  public static final DeferredHolder<RecipeType<?>, ? extends RecipeType<MoldingRecipe>> MOLDING_BASIN = register("molding_basin");

  // smeltery
  public static final DeferredHolder<RecipeType<?>, ? extends RecipeType<IMeltingRecipe>> MELTING = register("melting");
  public static final DeferredHolder<RecipeType<?>, ? extends RecipeType<EntityMeltingRecipe>> ENTITY_MELTING = register("entity_melting");
  public static final DeferredHolder<RecipeType<?>, ? extends RecipeType<MeltingFuel>> FUEL = register("fuel");
  public static final DeferredHolder<RecipeType<?>, ? extends RecipeType<AlloyRecipe>> ALLOYING = register("alloying");

  // modifiers
  public static final DeferredHolder<RecipeType<?>, ? extends RecipeType<SeveringRecipe>> SEVERING = register("severing");

  /** Internal recipe type for recipes that are not pulled by any specific crafting block */
  public static final DeferredHolder<RecipeType<?>, ? extends RecipeType<Recipe<?>>> DATA = register("data");

  /** All Tinkers recipe types whose values may be needed by the client book or JEI. */
  public static List<RecipeType<?>> getAllTypes() {
    return List.of(
      PART_BUILDER.get(), MATERIAL.get(), TINKER_STATION.get(), MODIFIER_WORKTABLE.get(),
      CASTING_BASIN.get(), CASTING_TABLE.get(), MOLDING_TABLE.get(), MOLDING_BASIN.get(),
      MELTING.get(), ENTITY_MELTING.get(), FUEL.get(), ALLOYING.get(), SEVERING.get(), DATA.get()
    );
  }

  /** Recipe types needed by the smeltery and foundry client displays. */
  public static List<RecipeType<?>> getSmelteryTypes() {
    return List.of(MELTING.get(), ENTITY_MELTING.get(), FUEL.get(), ALLOYING.get());
  }

  /** Recipe types used by casting and molding displays. */
  public static List<RecipeType<?>> getCastingTypes() {
    return List.of(CASTING_BASIN.get(), CASTING_TABLE.get(), MOLDING_TABLE.get(), MOLDING_BASIN.get());
  }

  /** Initializes the deferred register */
  public static void init(IEventBus bus) {
    TYPES.register(bus);
  }

  /**
   * Registers a new recipe type, prefixing with the mod ID
   * @param name  Recipe type name
   * @param <T>   Recipe type
   * @return  Registered recipe type
   */
  static <T extends Recipe<?>> DeferredHolder<RecipeType<?>, ? extends RecipeType<T>> register(String name) {
    return TYPES.register(name, () -> new RecipeType<>() {
      @Override
      public String toString() {
        return TConstruct.MOD_ID + ":" + name;
      }
    });
  }
}
