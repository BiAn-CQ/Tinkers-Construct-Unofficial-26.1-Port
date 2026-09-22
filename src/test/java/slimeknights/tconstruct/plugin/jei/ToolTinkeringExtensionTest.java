package slimeknights.tconstruct.plugin.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.recipe.tinkerstation.AbstractCraftingTinkeringRecipe;
import slimeknights.tconstruct.plugin.jei.modifiers.ToolTinkeringExtension;
import slimeknights.tconstruct.test.CoreTestBootstrap;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ToolTinkeringExtensionTest extends CoreTestBootstrap {
  @Test
  void craftingLinksMaterialInputAfterPrependedToolSlot() {
    AbstractCraftingTinkeringRecipe recipe = mock(AbstractCraftingTinkeringRecipe.class);
    List<ItemStack> tools = List.of(new ItemStack(Items.IRON_SWORD), new ItemStack(Items.DIAMOND_SWORD));
    List<ItemStack> materials = List.of(new ItemStack(Items.IRON_INGOT), new ItemStack(Items.DIAMOND));
    when(recipe.getInputCount()).thenReturn(1);
    when(recipe.getToolWithoutModifier(any(), anyBoolean())).thenReturn(tools);
    when(recipe.getToolWithModifier(any(), anyBoolean())).thenReturn(tools);
    when(recipe.getDisplayItems(eq(0), any(), anyBoolean())).thenReturn(materials);
    when(recipe.getToolWithoutModifier()).thenReturn(tools);
    when(recipe.getDisplayItems(0)).thenReturn(materials);
    when(recipe.linkToOutput()).thenReturn(new int[] {0});
    when(recipe.isToolCatalyst()).thenReturn(true);
    IFocusGroup focuses = mock(IFocusGroup.class);
    when(focuses.getItemStackFocuses()).thenAnswer(inv -> Stream.empty());
    IRecipeLayoutBuilder builder = mock(IRecipeLayoutBuilder.class);
    IRecipeSlotBuilder output = mock(IRecipeSlotBuilder.class, RETURNS_SELF);
    when(builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 95, 19)).thenReturn(output);
    List<IRecipeSlotBuilder> slots = IntStream.range(0, 9)
      .mapToObj(i -> mock(IRecipeSlotBuilder.class, RETURNS_SELF)).toList();
    ICraftingGridHelper grid = mock(ICraftingGridHelper.class);
    when(grid.createAndSetInputs(eq(builder), eq(VanillaTypes.ITEM_STACK), anyList(), eq(2), eq(2))).thenReturn(slots);
    var holder = new RecipeHolder<>(ResourceKey.create(Registries.RECIPE,
      Identifier.fromNamespaceAndPath("tconstruct", "repair_display")), recipe);

    ToolTinkeringExtension.INSTANCE.setRecipe(holder, builder, grid, focuses);

    int toolIndex = slimeknights.mantle.plugin.jei.MantleJEIConstants.getCraftingIndex(0, 2, 2);
    int materialIndex = slimeknights.mantle.plugin.jei.MantleJEIConstants.getCraftingIndex(1, 2, 2);
    verify(builder).createFocusLink(output, slots.get(toolIndex), slots.get(materialIndex));
    assertThat(ToolTinkeringExtension.INSTANCE.getIngredients(holder)).hasSize(2);
  }
}
