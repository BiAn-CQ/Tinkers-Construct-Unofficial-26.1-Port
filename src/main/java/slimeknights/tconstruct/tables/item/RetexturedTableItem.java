package slimeknights.tconstruct.tables.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import slimeknights.mantle.util.RetexturedHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Block item for tables whose appearance is selected by the texture stored on the stack.
 *
 * <p>In 26.1, {@link BlockItem} owns the item tooltip API and no longer forwards it to
 * the block. The old block-side tooltip hook therefore cannot expose the selected table
 * material by itself.</p>
 */
public class RetexturedTableItem extends BlockItem {
  public RetexturedTableItem(Block block, Item.Properties properties) {
    super(block, properties);
  }

  @Override
  public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                              Consumer<Component> tooltip, TooltipFlag flag) {
    super.appendHoverText(stack, context, display, tooltip, flag);
    List<Component> lines = new ArrayList<>();
    RetexturedHelper.addTooltip(stack, lines, flag);
    lines.forEach(tooltip);
  }
}
