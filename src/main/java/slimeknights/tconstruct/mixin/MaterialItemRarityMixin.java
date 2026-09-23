package slimeknights.tconstruct.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.modifiers.modules.build.RarityModule;

/** Restores material and tool rarity after removal of Item's stack-sensitive rarity hook. */
@Mixin(ItemStack.class)
public abstract class MaterialItemRarityMixin {
  @Inject(method = "getRarity", at = @At("HEAD"), cancellable = true)
  private void tconstruct$materialRarity(CallbackInfoReturnable<Rarity> callback) {
    ItemStack stack = (ItemStack)(Object)this;
    if (stack.getItem() instanceof IMaterialItem item) {
      callback.setReturnValue(MaterialRegistry.getMaterial(item.getMaterial(stack).getId()).getRarity());
    } else if (stack.getItem() instanceof IModifiable) {
      callback.setReturnValue(RarityModule.getRarity(stack));
    }
  }
}
