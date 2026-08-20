package slimeknights.tconstruct.tools.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationContainer;
import slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipe;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.LazyToolStack;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.TinkerModifiers;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

/** Recipe for transforming tipped arrows into a tool */
public class TippedToolTransformRecipe extends ToolBuildingRecipe {
  /** Loader instance */
  public static final RecordLoadable<TippedToolTransformRecipe> LOADER = RecordLoadable.create(
    ContextKey.ID.requiredField(),
    LoadableRecipeSerializer.RECIPE_GROUP, RESULT_FIELD, LAYOUT_FIELD,
    IngredientLoadable.DISALLOW_EMPTY.requiredField("input", r -> r.ingredients.get(0)),
    MaterialVariantId.LOADABLE.list(0).defaultField("materials", List.of(), false, r -> r.materials),
    ModifierId.PARSER.requiredField("modifier", r -> r.modifier),
    TippedToolTransformRecipe::new);

  protected final ModifierId modifier;
  public TippedToolTransformRecipe(Identifier id, String group, IModifiable output, @Nullable Identifier layoutSlot, Ingredient ingredient, List<MaterialVariantId> materials, ModifierId modifier) {
    super(id, group, output, 1, layoutSlot, List.of(ingredient), List.of(), materials);
    this.modifier = modifier;
  }

  @Override
  public RecipeSerializer getSerializer() {
    return TinkerModifiers.tippedToolTransformRecipeSerializer.get();
  }

  @Override
  public RecipeResult<LazyToolStack> getValidatedResult(ITinkerStationContainer inv, HolderLookup.Provider access) {
    RecipeResult<LazyToolStack> result = super.getValidatedResult(inv, access);
    if (result.isSuccess()) {
      // tool must have modifier, else we are adding bad data
      IToolStackView tool = result.getResult().getTool();
      if (tool.getModifierLevel(modifier) > 0) {
        // find the potion, should be first non-empty stack
        ItemStack stack = ItemStack.EMPTY;
        for (int i = 0; i < inv.getInputCount(); i++) {
          stack = inv.getInput(i);
          if (!stack.isEmpty()) {
            break;
          }
        }
        // if we found one, set its NBT into the result tool
        if (!stack.isEmpty()) {
          PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
          if (contents != null) {
            contents.potion().flatMap(holder -> holder.unwrapKey()).ifPresent(
              key -> tool.getPersistentData().putString(modifier, key.identifier().toString()));
          }
        }
      }
    }
    return result;
  }

  @Override
  public List<ItemStack> getDisplayOutput() {
    if (displayOutput == null) {
      ItemStack result = super.getDisplayOutput().get(0);
      displayOutput = Arrays.stream(slimeknights.tconstruct.library.recipe.TinkerIngredients.getItems(ingredients.get(0)))
        .map(stack -> {
          PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
          if (contents != null) {
            ItemStack copy = result.copy();
            contents.potion().flatMap(holder -> holder.unwrapKey()).ifPresent(
              key -> ToolStack.from(copy).getPersistentData().putString(modifier, key.identifier().toString()));
            return copy;
          }
          return result;
        }).toList();
    }
    return displayOutput;
  }
}
