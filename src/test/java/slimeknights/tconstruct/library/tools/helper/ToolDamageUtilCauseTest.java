package slimeknights.tconstruct.library.tools.helper;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.hook.behavior.ToolDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.test.CoreTestBootstrap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ToolDamageUtilCauseTest extends CoreTestBootstrap {
  private void assertCausePreserved(LivingEntity holder) {
    var tool = mock(IToolStackView.class);
    var entry = mock(ModifierEntry.class);
    var hook = mock(ToolDamageModifierHook.class);
    var cause = new ModifierId("test", "damage_source");
    when(tool.hasTag(TinkerTags.Items.DURABILITY)).thenReturn(true);
    when(tool.getModifierList()).thenReturn(List.of(entry));
    when(entry.getHook(ModifierHooks.TOOL_DAMAGE)).thenReturn(hook);

    assertThat(ToolDamageUtil.damageAnimated(tool, 4, holder, cause)).isFalse();

    verify(hook).beforeDamageTool(tool, entry, 4, holder, ItemStack.EMPTY, cause);
  }

  @Test
  void damageWithoutAnEntityKeepsTheModifierCause() {
    assertCausePreserved(null);
  }

  @Test
  void damageWithoutAMatchingEquipmentStackKeepsTheModifierCause() {
    var holder = mock(LivingEntity.class);
    when(holder.getItemBySlot(any())).thenReturn(ItemStack.EMPTY);
    assertCausePreserved(holder);
  }
}
