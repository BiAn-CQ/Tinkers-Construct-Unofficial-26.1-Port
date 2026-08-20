package slimeknights.tconstruct.library.compat;

import net.minecraft.tags.TagKey;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/** Compatibility values for vanilla mining tiers used by legacy data APIs. */
public enum Tiers implements Tier {
  WOOD(ToolMaterial.WOOD),
  STONE(ToolMaterial.STONE),
  IRON(ToolMaterial.IRON),
  GOLD(ToolMaterial.GOLD),
  DIAMOND(ToolMaterial.DIAMOND),
  NETHERITE(ToolMaterial.NETHERITE);

  private final ToolMaterial material;

  Tiers(ToolMaterial material) {
    this.material = material;
  }

  @Override
  public int getUses() {
    return material.durability();
  }

  @Override
  public float getSpeed() {
    return material.speed();
  }

  @Override
  public float getAttackDamageBonus() {
    return material.attackDamageBonus();
  }

  @Override
  public TagKey<Block> getIncorrectBlocksForDrops() {
    return material.incorrectBlocksForDrops();
  }

  /** Legacy harvest-level tag used by the 1.20 data generators. */
  public TagKey<Block> getTag() {
    return switch (this) {
      case WOOD -> net.neoforged.neoforge.common.Tags.Blocks.NEEDS_WOOD_TOOL;
      case GOLD -> net.neoforged.neoforge.common.Tags.Blocks.NEEDS_GOLD_TOOL;
      case STONE -> net.minecraft.tags.BlockTags.NEEDS_STONE_TOOL;
      case IRON -> net.minecraft.tags.BlockTags.NEEDS_IRON_TOOL;
      case DIAMOND -> net.minecraft.tags.BlockTags.NEEDS_DIAMOND_TOOL;
      case NETHERITE -> net.neoforged.neoforge.common.Tags.Blocks.NEEDS_NETHERITE_TOOL;
    };
  }

  @Override
  public int getEnchantmentValue() {
    return material.enchantmentValue();
  }

  @Override
  public Ingredient getRepairIngredient() {
    return Ingredient.of(HolderSet.emptyNamed(BuiltInRegistries.ITEM, material.repairItems()));
  }
}
