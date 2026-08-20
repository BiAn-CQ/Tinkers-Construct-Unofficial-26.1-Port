package slimeknights.tconstruct.library.modifiers.hook.display;

import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.common.TinkerTags;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DurabilityDisplayModifierHookTest {
  @Test
  void virtualDurabilityDoesNotRequireVanillaDamageComponents() {
    ItemStack stack = mock(ItemStack.class);
    when(stack.is(TinkerTags.Items.DURABILITY)).thenReturn(true);
    when(stack.isDamageableItem()).thenReturn(false);

    assertThat(DurabilityDisplayModifierHook.supportsDurabilityBar(stack)).isTrue();
  }

  @Test
  void unrelatedItemsDoNotUseTheTinkersDurabilityBar() {
    ItemStack stack = mock(ItemStack.class);
    when(stack.is(TinkerTags.Items.DURABILITY)).thenReturn(false);

    assertThat(DurabilityDisplayModifierHook.supportsDurabilityBar(stack)).isFalse();
  }
}
