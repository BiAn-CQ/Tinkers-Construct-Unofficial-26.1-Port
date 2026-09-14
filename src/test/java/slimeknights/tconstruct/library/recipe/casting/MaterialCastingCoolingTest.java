package slimeknights.tconstruct.library.recipe.casting;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.recipe.casting.material.DisplayMaterialCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.material.DisplayMaterialCastingRecipe.CompositeFluid;
import slimeknights.tconstruct.test.CoreTestBootstrap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MaterialCastingCoolingTest extends CoreTestBootstrap {
  @Test
  void castingUsesFluidIdentityRatherThanStackAsLookupKey() {
    var times = new Object2IntOpenHashMap<Fluid>();
    times.put(Fluids.WATER, 20);
    times.put(Fluids.LAVA, 100);
    var display = DisplayMaterialCastingRecipe.type(mock(RecipeType.class)).casting(times);
    assertThat(display.getCoolingTime(new FluidStack(Fluids.WATER, 90))).isEqualTo(20);
    assertThat(display.getCoolingTime(new FluidStack(Fluids.LAVA, 90))).isEqualTo(100);
    assertThat(display.getCoolingTime()).isEqualTo(100);
    assertThat(display.linkCastToOutput()).isFalse();
    assertThat(display.linkFluidsToOutput()).isTrue();
  }

  @Test
  void compositeDistinguishesDifferentAmountsOfSameFluid() {
    var times = new Object2IntOpenHashMap<CompositeFluid>();
    times.put(new CompositeFluid(Fluids.WATER, 90), 20);
    times.put(new CompositeFluid(Fluids.WATER, 180), 40);
    var display = DisplayMaterialCastingRecipe.type(mock(RecipeType.class)).composite(times);
    assertThat(display.getCoolingTime(new FluidStack(Fluids.WATER, 90))).isEqualTo(20);
    assertThat(display.getCoolingTime(new FluidStack(Fluids.WATER, 180))).isEqualTo(40);
    assertThat(display.getCoolingTime(new FluidStack(Fluids.WATER, 270))).isEqualTo(40);
    assertThat(display.linkCastToOutput()).isTrue();
  }
}
