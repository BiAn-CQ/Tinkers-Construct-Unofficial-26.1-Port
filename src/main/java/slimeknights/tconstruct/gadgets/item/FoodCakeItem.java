package slimeknights.tconstruct.gadgets.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import slimeknights.mantle.item.BlockTooltipItem;
import slimeknights.tconstruct.gadgets.block.FoodCakeBlock;

import java.util.function.Consumer;

/** Cake block item that includes the block's configured food effects. */
public class FoodCakeItem extends BlockTooltipItem {
  private final FoodCakeBlock cake;

  public FoodCakeItem(FoodCakeBlock cake, Properties properties) {
    super(cake, properties);
    this.cake = cake;
  }

  @Override
  public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
    super.appendHoverText(stack, context, display, tooltip, flag);
    cake.appendHoverText(stack, context, display, tooltip, flag);
  }
}
