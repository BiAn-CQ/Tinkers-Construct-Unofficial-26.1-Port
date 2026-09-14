package slimeknights.tconstruct.plugin.jei.material;

import com.google.common.collect.Streams;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.nbt.MaterialIdNBT;
import slimeknights.tconstruct.plugin.jei.util.CategoryUtil;
import java.util.ArrayList;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.plugin.jei.MantleJEIConstants;
import slimeknights.tconstruct.library.recipe.TinkerIngredients;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;
import slimeknights.tconstruct.library.recipe.material.MaterialsCraftingTableRecipe;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/** JEI 29 extension shared by shaped and shapeless material-aware recipes. */
public abstract class MaterialsCraftingExtension<T extends CraftingRecipe & MaterialsCraftingTableRecipe>
  implements ICraftingCategoryExtension<T> {

  /** Returns the recipe inputs in crafting-grid order. */
  protected abstract List<Ingredient> getInputIngredients(T recipe);

  /** Gets the grid slots whose displayed material should follow the output. */
  protected int[] getMaterialSlots(T recipe, Ingredient firstPart) {
    return new int[] {0};
  }

  @Override
  public boolean isHandled(RecipeHolder<T> holder) {
    T recipe = holder.value();
    List<Ingredient> parts = recipe.getParts();
    for (int i = 0; i < recipe.getPartCount(); i++) {
      if (TinkerIngredients.getItems(parts.get(i)).length == 0) {
        return false;
      }
    }
    return true;
  }

  @Override
  public List<SlotDisplay> getIngredients(RecipeHolder<T> holder) {
    return getInputIngredients(holder.value()).stream().map(Ingredient::display).toList();
  }

  @Override
  public void setRecipe(RecipeHolder<T> holder, IRecipeLayoutBuilder builder,
                        ICraftingGridHelper craftingGridHelper, IFocusGroup focuses) {
    T recipe = holder.value();
    ItemStack plainResult = recipe.assemble(CraftingInput.EMPTY);
    List<ItemStack> result;
    @Nullable int[] materialSlots;

    if (recipe.getPartCount() == 1) {
      Ingredient firstPart = recipe.getParts().getFirst();
      result = Arrays.stream(TinkerIngredients.getItems(firstPart)).map(variant -> {
        ItemStack stack = plainResult.copy();
        if (variant.getItem() instanceof IMaterialItem materialItem) {
          recipe.setMaterial(stack, materialItem.getMaterial(variant));
        } else {
          recipe.setMaterial(stack, MaterialRecipeCache.findRecipe(variant).getMaterial().getVariant());
        }
        return stack;
      }).toList();
      materialSlots = getMaterialSlots(recipe, firstPart);
    } else if (recipe.getExtraMaterials().isEmpty() && plainResult.getItem() instanceof IMaterialItem materialItem) {
      result = List.of(materialItem.setMaterialForced(plainResult, ToolBuildHandler.getRenderMaterial(0)));
      materialSlots = null;
    } else {
      result = List.of(IModifiableDisplay.getDisplayStack(plainResult));
      materialSlots = null;
    }

    List<List<ItemStack>> inputs = new ArrayList<>(getInputIngredients(recipe).stream()
      .map(ingredient -> List.of(TinkerIngredients.getItems(ingredient))).toList());
    ItemStack focus = CategoryUtil.getResultItemFocus(focuses);
    if (!focus.isEmpty() && recipe.getPartCount() > 1) {
      MaterialIdNBT materials = MaterialIdNBT.from(focus);
      for (int i = 0; i < recipe.getPartCount(); i++) {
        Ingredient part = recipe.getParts().get(i);
        MaterialVariantId material = materials.getMaterial(i);
        List<ItemStack> matches = filterMaterialInputs(List.of(TinkerIngredients.getItems(part)), material);
        for (int slot : getMaterialSlots(recipe, part)) {
          inputs.set(slot, matches);
        }
      }
    }
    setRecipeStacks(builder, craftingGridHelper, inputs, result, plainResult,
      getWidth(holder), getHeight(holder), materialSlots);
  }

  /** Builds a material-aware crafting layout and links material input slots to the output. */
  public static void setRecipe(IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper,
                               List<Ingredient> ingredients, List<ItemStack> result, ItemStack plainResult,
                               int width, int height, @Nullable int[] materialSlots) {
    List<List<ItemStack>> inputStacks = ingredients.stream()
      .map(ingredient -> List.of(TinkerIngredients.getItems(ingredient))).toList();
    setRecipeStacks(builder, craftingGridHelper, inputStacks, result, plainResult, width, height, materialSlots);
  }

  private static void setRecipeStacks(IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper,
                                     List<List<ItemStack>> inputStacks, List<ItemStack> result, ItemStack plainResult,
                                     int width, int height, @Nullable int[] materialSlots) {
    builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(plainResult);
    if (width <= 0 || height <= 0) {
      width = height = getShapelessSize(inputStacks.size());
      builder.setShapeless();
    }
    List<IRecipeSlotBuilder> inputs = craftingGridHelper.createAndSetInputs(
      builder, VanillaTypes.ITEM_STACK, inputStacks, width, height);
    IRecipeSlotBuilder output = craftingGridHelper.createAndSetOutputs(builder, result).setSlotName("result");
    if (inputs.size() != 9) {
      Mantle.logger.error("Failed to create focus link for material recipe as the layout {} is not 3x3",
        builder.getClass().getName());
    } else if (materialSlots != null) {
      int finalWidth = width;
      int finalHeight = height;
      builder.createFocusLink(Streams.concat(
        Stream.of(output),
        Arrays.stream(materialSlots)
          .mapToObj(i -> inputs.get(MantleJEIConstants.getCraftingIndex(i, finalWidth, finalHeight)))
      ).toArray(IRecipeSlotBuilder[]::new));
    }
  }

  private static int getShapelessSize(int total) {
    if (total > 4) return 3;
    if (total > 1) return 2;
    return 1;
  }

  static List<ItemStack> filterMaterialInputs(List<ItemStack> available, MaterialVariantId material) {
    List<ItemStack> matching = available.stream()
      .filter(stack -> material.matchesVariant(MaterialRecipeCache.getMaterial(stack))).toList();
    return matching.isEmpty() ? available : matching;
  }

  @Override
  public void onDisplayedIngredientsUpdate(RecipeHolder<T> holder, List<IRecipeSlotDrawable> slots, IFocusGroup focuses) {
    T recipe = holder.value();
    if (recipe.getPartCount() <= 1) {
      return;
    }
    IRecipeSlotDrawable output = CategoryUtil.findSlot(slots, "result");
    if (output == null) {
      return;
    }
    int width = getWidth(holder);
    int height = getHeight(holder);
    if (width <= 0 || height <= 0) {
      width = height = getShapelessSize(getInputIngredients(recipe).size());
    }
    List<MaterialVariantId> materials = new ArrayList<>();
    for (Ingredient part : recipe.getParts()) {
      int index = getMaterialSlots(recipe, part)[0];
      ItemStack input = slots.get(MantleJEIConstants.getCraftingIndex(index, width, height))
        .getDisplayedItemStack().orElse(ItemStack.EMPTY);
      materials.add(MaterialRecipeCache.getMaterial(input));
    }
    materials.addAll(recipe.getExtraMaterials());
    output.createDisplayOverrides().addItemStack(
      new MaterialIdNBT(materials).updateStack(recipe.assemble(CraftingInput.EMPTY).copy()));
  }
}
