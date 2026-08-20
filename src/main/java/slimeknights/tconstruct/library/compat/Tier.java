package slimeknights.tconstruct.library.compat;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/** Compatibility view of the pre-26.1 mining tier API. */
public interface Tier {
  int getUses();
  float getSpeed();
  float getAttackDamageBonus();
  TagKey<Block> getIncorrectBlocksForDrops();
  int getEnchantmentValue();
  Ingredient getRepairIngredient();
}
