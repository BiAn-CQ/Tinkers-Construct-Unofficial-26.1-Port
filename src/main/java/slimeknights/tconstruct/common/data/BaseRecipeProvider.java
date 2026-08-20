package slimeknights.tconstruct.common.data;

import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.data.DataProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.recipe.data.IRecipeHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.utils.ResourceId;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * Shared logic for each module's recipe provider
 */
public abstract class BaseRecipeProvider extends RecipeProvider implements IRecipeHelper {
  public BaseRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
    super(registries, output);
    TConstruct.sealTinkersClass(this, "BaseRecipeProvider", "BaseRecipeProvider is trivial to recreate and directly extending can lead to addon recipes polluting our namespace.");
  }

  @Override
  protected final void buildRecipes() {
    buildTinkersRecipes(output);
  }

  /** Builds recipes through the native 1.21 recipe sink. */
  protected abstract void buildTinkersRecipes(RecipeOutput output);

  /** Registry context supplied by the native 1.21 recipe provider. */
  protected final HolderLookup.Provider registries() {
    return registries;
  }

  /** Creates the 26.1 data-provider runner used to supply the registry and recipe sinks. */
  public static DataProvider runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
                                    String name, BiFunction<HolderLookup.Provider,RecipeOutput,? extends BaseRecipeProvider> factory) {
    return new RecipeProvider.Runner(output, registries) {
      @Override
      protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput recipeOutput) {
        return factory.apply(registries, recipeOutput);
      }

      @Override
      public String getName() {
        return "Tinkers' Construct " + name;
      }
    };
  }

  /** Resolves the visibility conflict between the native provider and Mantle's recipe helper. */
  @Override
  public Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike item) {
    return super.has(item);
  }

  /** Resolves the visibility conflict between the native provider and Mantle's recipe helper. */
  @Override
  public Criterion<InventoryChangeTrigger.TriggerInstance> has(TagKey<Item> tag) {
    return IRecipeHelper.super.has(tag);
  }

  @Override
  public String getModId() {
    return TConstruct.MOD_ID;
  }

  /** Extends a typed Tinkers ID while preserving the recipe provider namespace. */
  protected Identifier wrap(ResourceId id, String prefix, String suffix) {
    return wrap(id.location(), prefix, suffix);
  }

  /** Prefixes a typed Tinkers ID. */
  protected Identifier prefix(ResourceId id, String prefix) {
    return prefix(id.location(), prefix);
  }

  /** Suffixes a typed Tinkers ID. */
  protected Identifier suffix(ResourceId id, String suffix) {
    return suffix(id.location(), suffix);
  }
}
