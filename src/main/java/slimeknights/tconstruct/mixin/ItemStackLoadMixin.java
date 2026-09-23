package slimeknights.tconstruct.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.library.tools.part.MaterialItem;
import slimeknights.tconstruct.library.utils.ItemStackDataUtil;

import java.util.function.Function;

/** Restores data-driven item validation when decoding saved stacks, without changing network decoding or copying. */
@Mixin(ItemStack.class)
public abstract class ItemStackLoadMixin {
  @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE",
    target = "Lcom/mojang/serialization/MapCodec;recursive(Ljava/lang/String;Ljava/util/function/Function;)Lcom/mojang/serialization/MapCodec;"))
  private static MapCodec<ItemStack> tconstruct$validateSavedStack(MapCodec<ItemStack> codec) {
    return codec.xmap(stack -> {
      if (stack.getItem() instanceof IModifiable item) {
        var tag = ItemStackDataUtil.getTag(stack);
        if (tag != null) {
          ToolStack.verifyTag(stack.getItem(), tag, item.getToolDefinition());
          ItemStackDataUtil.setTag(stack, tag);
          item.updateDynamicComponents(stack);
        }
      } else if (stack.getItem() instanceof IMaterialItem && MaterialRegistry.isFullyLoaded()) {
        var tag = ItemStackDataUtil.getTag(stack);
        if (tag != null) {
          MaterialItem.verifyTag(tag);
          ItemStackDataUtil.setTag(stack, tag);
        }
      }
      return stack;
    }, Function.identity());
  }
}
