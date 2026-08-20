package slimeknights.tconstruct.library.data.recipe;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;

import javax.annotation.Nullable;

/** @deprecated compatibility result for the pre-1.21 FinishedRecipe API. */
@Deprecated(forRemoval = true)
public abstract class AbstractRecipeOutput {
  protected final Identifier id;
  @Nullable
  protected final Identifier advancementId;

  protected AbstractRecipeOutput(Identifier id, @Nullable Identifier advancementId) {
    this.id = id;
    this.advancementId = advancementId;
  }

  public abstract RecipeSerializer<?> getType();

  public abstract void serializeRecipeData(JsonObject json);

  public Identifier getId() { return id; }
  @Nullable public Identifier getAdvancementId() { return advancementId; }
  @Nullable public JsonObject serializeAdvancement() { return null; }
}
