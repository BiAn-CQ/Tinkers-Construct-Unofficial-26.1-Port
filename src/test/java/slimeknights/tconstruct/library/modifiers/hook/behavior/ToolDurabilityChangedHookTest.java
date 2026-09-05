package slimeknights.tconstruct.library.modifiers.hook.behavior;

import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ToolDurabilityChangedHookTest {
  @Test
  void mergerNotifiesEveryHookForDamageAndRepair() {
    ToolDurabilityChangedHook first = mock(ToolDurabilityChangedHook.class);
    ToolDurabilityChangedHook second = mock(ToolDurabilityChangedHook.class);
    IToolStackView tool = mock(IToolStackView.class);
    ModifierEntry modifier = mock(ModifierEntry.class);
    ItemStack stack = mock(ItemStack.class);
    ToolDurabilityChangedHook merger = new ToolDurabilityChangedHook.Merger(List.of(first, second));
    merger.afterDamageTool(tool, modifier, 3, null, stack);
    merger.afterRepairTool(tool, modifier, 2);
    for (ToolDurabilityChangedHook hook : List.of(first, second)) {
      verify(hook).afterDamageTool(tool, modifier, 3, null, stack);
      verify(hook).afterRepairTool(tool, modifier, 2);
    }
  }
}
