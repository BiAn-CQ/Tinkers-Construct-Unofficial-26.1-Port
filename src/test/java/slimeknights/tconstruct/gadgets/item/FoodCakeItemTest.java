package slimeknights.tconstruct.gadgets.item;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.gadgets.TinkerGadgets;
import slimeknights.tconstruct.gadgets.block.FoodCakeBlock;
import slimeknights.tconstruct.shared.TinkerFood;
import slimeknights.tconstruct.test.BaseMcTest;
import slimeknights.tconstruct.world.block.FoliageType;

import static org.assertj.core.api.Assertions.assertThat;

class FoodCakeItemTest extends BaseMcTest {
  @Test void registeredCakeItemsShowConfiguredEffectsThroughTheRealItemTooltip() {
    for (var type : FoliageType.values()) {
      verifyTooltip(TinkerGadgets.cake.get(type), TinkerFood.getCake(type));
    }
    verifyTooltip(TinkerGadgets.magmaCake.get(), TinkerFood.MAGMA_CAKE);
  }

  private static void verifyTooltip(FoodCakeBlock block, TinkerFood.Entry food) {
    var stack = new ItemStack(block);
    var tooltip = stack.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.NORMAL);
    for (var effect : food.effects()) {
      assertThat(tooltip).anySatisfy(line ->
        assertThat(line.getContents()).isInstanceOfSatisfying(TranslatableContents.class, duration -> {
          assertThat(duration.getKey()).isEqualTo("potion.withDuration");
          assertThat(duration.getArgs()[0]).isInstanceOfSatisfying(Component.class, name ->
            assertThat(name.getContents()).isInstanceOfSatisfying(TranslatableContents.class, text ->
              assertThat(text.getKey()).isEqualTo(effect.getDescriptionId())));
        }));
    }
  }
}
