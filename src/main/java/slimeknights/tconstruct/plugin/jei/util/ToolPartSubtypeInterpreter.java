package slimeknights.tconstruct.plugin.jei.util;

import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

/**
 * Subtype interpreter for material items, considers variants in ingredient context but ignores them in recipe context.
 */
public enum ToolPartSubtypeInterpreter implements ISubtypeInterpreter<ItemStack> {
  INSTANCE;

  @Override
  public String getSubtypeData(ItemStack stack, UidContext context) {
    MaterialVariantId materialId = IMaterialItem.getMaterialFromStack(stack);
    if (materialId.equals(IMaterial.UNKNOWN_ID)) {
      return null;
    }
    if (context == UidContext.Ingredient) {
      return materialId.toString();
    }
    return materialId.getId().toString();
  }
}
