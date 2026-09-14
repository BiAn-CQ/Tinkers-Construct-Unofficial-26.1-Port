package slimeknights.tconstruct.tables.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationContainer;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RepairRecipePriorityTest {
  @Test
  void undamagedToolDefersWithoutInspectingRepairMaterials() {
    ITinkerStationContainer inventory = mock(ITinkerStationContainer.class);
    ItemStack stack = mock(ItemStack.class);
    ToolStack tool = mock(ToolStack.class);
    when(inventory.getTinkerableStack()).thenReturn(stack);
    when(stack.is(TinkerTags.Items.DURABILITY)).thenReturn(true);
    when(inventory.getTinkerable()).thenReturn(tool);

    assertThat(new TinkerStationRepairRecipe().matches(inventory, mock(Level.class))).isFalse();
    verify(inventory, never()).getInputCount();
  }
}
