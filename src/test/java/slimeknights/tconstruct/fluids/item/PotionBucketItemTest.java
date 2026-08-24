package slimeknights.tconstruct.fluids.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.test.BaseMcTest;
import slimeknights.tconstruct.test.TestHelper;

import static org.assertj.core.api.Assertions.assertThat;

class PotionBucketItemTest extends BaseMcTest {
  @Test
  void potionBucketHasNativeDrinkComponents() throws ReflectiveOperationException {
    PotionBucketItem item = (PotionBucketItem) TinkerFluids.potion.asItem();
    ItemStack stack = new ItemStack(Items.BUCKET);
    stack.applyComponents(TestHelper.defaultComponents(item));

    assertThat(stack.get(DataComponents.CONSUMABLE)).isNotNull();
    assertThat(item.getUseAnimation(stack)).isEqualTo(ItemUseAnimation.DRINK);
    assertThat(item.getUseDuration(stack, null)).isEqualTo(PotionBucketItem.DRINK_DURATION);
    assertThat(stack.get(DataComponents.POTION_DURATION_SCALE)).isEqualTo(PotionBucketItem.POTION_DURATION_SCALE);
  }
}
