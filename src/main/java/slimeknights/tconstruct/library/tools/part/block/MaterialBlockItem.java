package slimeknights.tconstruct.library.tools.part.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.library.tools.part.MaterialItem;
import slimeknights.tconstruct.library.utils.ItemStackDataUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

/** Implementation of {@link MaterialItem} on a {@link BlockItem}. */
public class MaterialBlockItem extends BlockItem implements IMaterialItem {
  public MaterialBlockItem(Block block, Properties properties) {
    super(block, properties);
  }

  @Override
  public MaterialVariantId getMaterial(ItemStack stack) {
    return MaterialItem.getMaterialId(ItemStackDataUtil.getTag(stack));
  }

  public Component getName(ItemStack stack) {
    return MaterialItem.getName(this, stack);
  }

  @Override
  public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
    List<Component> lines = new ArrayList<>();
    MaterialItem.appendHoverText(this, stack, lines, flag);
    lines.forEach(tooltip);
    super.appendHoverText(stack, context, display, tooltip, flag);
  }

  @Nullable
  public String getCreatorModId(ItemStack stack) {
    return MaterialItem.getCreatorModId(this, stack);
  }

  public void verifyComponentsAfterLoad(ItemStack stack) {
    CompoundTag tag = ItemStackDataUtil.getTag(stack);
    if (tag != null) {
      MaterialItem.verifyTag(tag);
      ItemStackDataUtil.setTag(stack, tag);
    }
  }
}
