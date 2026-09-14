package slimeknights.tconstruct.library.recipe.casting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DisplayCastingRecipeTest {
  @Test
  void builderChoosesWhichInputsFollowOutput() {
    RecipeType<?> type = mock(RecipeType.class);
    ItemStack output = mock(ItemStack.class);
    var input = List.of(mock(net.neoforged.neoforge.fluids.FluidStack.class));
    IDisplayableCastingRecipe fluids = DisplayCastingRecipe.type(type).fluids(input).result(output).linkFluidsToOutput().build();
    assertThat(fluids.linkFluidsToOutput()).isTrue();
    assertThat(fluids.linkCastToOutput()).isFalse();
    assertThat(fluids.getOutputs()).containsExactly(output);
    IDisplayableCastingRecipe all = DisplayCastingRecipe.type(type).fluids(input).result(output).linkAllToOutput().build();
    assertThat(all.linkCastToOutput()).isTrue();
    assertThat(all.linkFluidsToOutput()).isTrue();
    IDisplayableCastingRecipe unlinked = DisplayCastingRecipe.type(type).fluids(input).result(output).unlinkOutput().build();
    assertThat(unlinked.linkCastToOutput()).isFalse();
    assertThat(unlinked.linkFluidsToOutput()).isFalse();
  }

  @SuppressWarnings("deprecation")
  @Test
  void legacyConstructorKeepsStaticCoolingAndCastLinkDefaults() {
    ItemStack output = mock(ItemStack.class);
    DisplayCastingRecipe recipe = new DisplayCastingRecipe(null, mock(RecipeType.class), List.of(), List.of(), output, 40, false);
    assertThat(recipe.getCoolingTime()).isEqualTo(40);
    assertThat(recipe.isCoolingTimeDynamic()).isFalse();
    assertThat(recipe.isSlotsDynamic()).isFalse();
    assertThat(recipe.linkCastToOutput()).isTrue();
    assertThat(recipe.linkFluidsToOutput()).isFalse();
  }
}
