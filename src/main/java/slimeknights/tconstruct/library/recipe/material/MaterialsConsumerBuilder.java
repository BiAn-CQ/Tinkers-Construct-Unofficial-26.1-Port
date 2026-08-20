package slimeknights.tconstruct.library.recipe.material;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.ingredient.MaterialIngredient;
import slimeknights.tconstruct.library.recipe.ingredient.MaterialValueIngredient;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Converts vanilla crafting builder results into material-aware recipes. */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MaterialsConsumerBuilder {
  private final int shapedPartCount;
  private final int shapelessPartCount;
  private final List<MaterialVariantId> materials = new ArrayList<>();

  public static MaterialsConsumerBuilder shaped(String parts) {
    if (parts.isEmpty()) {
      throw new IllegalArgumentException("Parts may not be empty");
    }
    return new MaterialsConsumerBuilder(parts.length(), 0);
  }

  public static MaterialsConsumerBuilder shapeless(int parts) {
    if (parts <= 0) {
      throw new IllegalArgumentException("Parts must be greater than 0");
    }
    return new MaterialsConsumerBuilder(0, parts);
  }

  public MaterialsConsumerBuilder material(MaterialVariantId material) {
    materials.add(material);
    return this;
  }

  public RecipeOutput build(RecipeOutput output) {
    List<MaterialVariantId> extraMaterials = List.copyOf(materials);
    return new RecipeOutput() {
      @Override
      public Advancement.Builder advancement() {
        return output.advancement();
      }

      @Override
      public void includeRootAdvancement() {
        output.includeRootAdvancement();
      }

      @Override
      public void accept(ResourceKey<Recipe<?>> id, Recipe<?> recipe, @Nullable AdvancementHolder advancement) {
        accept(id, recipe, advancement, new ICondition[0]);
      }

      @Override
      public void accept(ResourceKey<Recipe<?>> id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
        Recipe<?> converted;
        if (shapelessPartCount > 0) {
          if (!(recipe instanceof ShapelessRecipe shapeless)) {
            throw new IllegalArgumentException("Material recipe requires a shapeless recipe, got " + recipe.getClass().getName());
          }
          converted = new ShapelessMaterialsRecipe(shapeless, shapelessPartCount, extraMaterials);
        } else {
          if (!(recipe instanceof ShapedRecipe shaped)) {
            throw new IllegalArgumentException("Material recipe requires a shaped recipe, got " + recipe.getClass().getName());
          }
          List<Ingredient> parts = shaped.getIngredients().stream().flatMap(java.util.Optional::stream)
            .filter(MaterialsConsumerBuilder::containsMaterialIngredient).distinct().toList();
          if (parts.size() != shapedPartCount) {
            throw new IllegalStateException("Expected " + shapedPartCount + " material part ingredients in " + id + ", found " + parts.size());
          }
          converted = new ShapedMaterialsRecipe(shaped, parts, extraMaterials);
        }
        output.accept(id, converted, advancement, conditions);
      }
    };
  }

  private static boolean containsMaterialIngredient(Ingredient ingredient) {
    if (!ingredient.isCustom()) {
      return false;
    }
    Object custom = ingredient.getCustomIngredient();
    if (custom instanceof MaterialIngredient || custom instanceof MaterialValueIngredient) {
      return true;
    }
    return custom instanceof CompoundIngredient compound && compound.children().stream().anyMatch(MaterialsConsumerBuilder::containsMaterialIngredient);
  }
}
