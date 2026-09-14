package slimeknights.tconstruct.plugin.jei.material;

import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MaterialFocusTest {
  @Test
  void filtersMatchingMaterialAndRetainsAvailableInputsForImpossibleFocus() {
    ItemStack ironStack = mock(ItemStack.class);
    ItemStack woodStack = mock(ItemStack.class);
    MaterialId iron = new MaterialId("tconstruct", "iron");
    MaterialId wood = new MaterialId("tconstruct", "wood");
    List<ItemStack> inputs = List.of(ironStack, woodStack);
    try (var cache = mockStatic(MaterialRecipeCache.class)) {
      cache.when(() -> MaterialRecipeCache.getMaterial(ironStack)).thenReturn(iron);
      cache.when(() -> MaterialRecipeCache.getMaterial(woodStack)).thenReturn(wood);
      assertThat(MaterialsCraftingExtension.filterMaterialInputs(inputs, iron)).containsExactly(ironStack);
      assertThat(MaterialsCraftingExtension.filterMaterialInputs(inputs, new MaterialId("tconstruct", "stone")))
        .isSameAs(inputs);
    }
  }
}
