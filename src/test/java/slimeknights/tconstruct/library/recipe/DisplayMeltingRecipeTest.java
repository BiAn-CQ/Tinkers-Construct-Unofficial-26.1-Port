package slimeknights.tconstruct.library.recipe;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.recipe.melting.DisplayMeltingRecipe;
import slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class DisplayMeltingRecipeTest extends slimeknights.tconstruct.test.CoreTestBootstrap {
  @Test
  void dynamicRecipeKeepsInputOutputAndByproductAmountsAligned() {
    var recipe = DisplayMeltingRecipe.id(Identifier.fromNamespaceAndPath("tconstruct", "test"))
      .inputs(List.of(new ItemStack(Items.STICK), new ItemStack(Items.BOWL)))
      .outputs(List.of(new FluidStack(Fluids.WATER, 90), new FluidStack(Fluids.WATER, 360)))
      .byproduct(List.of(new FluidStack(Fluids.LAVA, 10), new FluidStack(Fluids.LAVA, 40)))
      .temperature(800).timeDynamic().build();
    assertThat(recipe.getInputs()).hasSize(2);
    assertThat(recipe.getOutputs()).extracting(FluidStack::getAmount).containsExactly(90, 360);
    assertThat(recipe.getOutputWithByproducts().get(1)).extracting(FluidStack::getAmount).containsExactly(10, 40);
    assertThat(recipe.isTimeDynamic()).isTrue();
    assertThat(recipe.getTime()).isEqualTo(IMeltingRecipe.calcTimeForAmount(800, 360));
    assertThat(recipe.getTime(recipe.getOutputs().getFirst())).isEqualTo(IMeltingRecipe.calcTimeForAmount(800, 90));
  }
}
