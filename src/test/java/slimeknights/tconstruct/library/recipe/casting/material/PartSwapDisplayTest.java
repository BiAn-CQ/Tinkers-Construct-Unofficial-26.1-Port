package slimeknights.tconstruct.library.recipe.casting.material;

import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PartSwapDisplayTest {
  @Test
  void compositeRequiresBothInputAndOutputToBeSupported() {
    MaterialStatsId stats = mock(MaterialStatsId.class);
    MaterialVariant input = MaterialVariant.of((slimeknights.tconstruct.library.materials.definition.MaterialVariantId)new MaterialId("tconstruct", "iron"));
    MaterialVariant output = MaterialVariant.of((slimeknights.tconstruct.library.materials.definition.MaterialVariantId)new MaterialId("tconstruct", "gold"));
    var recipe = new PartSwapCastingRecipe.FluidRecipe(List.of(), input, output);
    when(stats.canUseMaterial(output.getId())).thenReturn(true);
    assertThat(PartSwapCastingRecipe.supportsDisplayMaterials(stats, recipe, true)).isFalse();
    assertThat(PartSwapCastingRecipe.supportsDisplayMaterials(stats, recipe, false)).isTrue();
    when(stats.canUseMaterial(input.getId())).thenReturn(true);
    assertThat(PartSwapCastingRecipe.supportsDisplayMaterials(stats, recipe, true)).isTrue();
    when(stats.canUseMaterial(output.getId())).thenReturn(false);
    assertThat(PartSwapCastingRecipe.supportsDisplayMaterials(stats, recipe, true)).isFalse();
  }
}
