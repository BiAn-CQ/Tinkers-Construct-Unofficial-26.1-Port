package slimeknights.tconstruct.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TConstructItemModelPropertiesTest {
  @Test
  void blockingUseReadsTheAuthoritativeStack() {
    Item item = mock(Item.class);
    ItemStack renderedStack = mock(ItemStack.class);
    when(renderedStack.getItem()).thenReturn(item);
    when(renderedStack.getUseAnimation()).thenReturn(ItemUseAnimation.NONE);
    ItemStack useStack = mock(ItemStack.class);
    when(useStack.is(item)).thenReturn(true);
    when(useStack.getUseAnimation()).thenReturn(ItemUseAnimation.BLOCK);
    LivingEntity holder = mock(LivingEntity.class);
    when(holder.asLivingEntity()).thenReturn(holder);
    when(holder.isUsingItem()).thenReturn(true);
    when(holder.getUseItem()).thenReturn(useStack);

    assertThat(TConstructItemModelProperties.getValue(
      "tconstruct:charging", renderedStack, null, holder, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, 0))
      .isEqualTo(2.0f);
  }
}
