package slimeknights.tconstruct.library.recipe.helper;

import com.google.gson.JsonObject;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.SimpleRecipeSerializer;
import slimeknights.mantle.recipe.data.SimpleRecipeBuilder;

/** Emits fieldless loadable recipes through the native 26.1 recipe sink. */
public final class SimpleRecipeOutput {
  private SimpleRecipeOutput() {}

  public static <T extends Recipe<?>> void save(RecipeOutput output, Identifier id, RecipeSerializer<T> serializer) {
    T recipe;
    if (SimpleRecipeSerializer.isSimple(serializer)) {
      recipe = SimpleRecipeSerializer.create(serializer);
    } else {
      recipe = LoadableRecipeSerializer.fromJson(serializer, id, new JsonObject());
    }
    SimpleRecipeBuilder.recipe(recipe).save(output, id);
  }
}
