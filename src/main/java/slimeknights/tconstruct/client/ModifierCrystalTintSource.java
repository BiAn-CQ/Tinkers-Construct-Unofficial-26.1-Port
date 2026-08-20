package slimeknights.tconstruct.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.client.ResourceColorManager;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.utils.Util;
import slimeknights.tconstruct.tools.item.ModifierCrystalItem;

/** Native 26.1 tint source for a modifier crystal's stored modifier. */
public enum ModifierCrystalTintSource implements ItemTintSource {
  INSTANCE;

  public static final MapCodec<ModifierCrystalTintSource> MAP_CODEC = MapCodec.unit(INSTANCE);

  @Override
  public int calculate(ItemStack stack, ClientLevel level, LivingEntity entity) {
    ModifierId modifier = ModifierCrystalItem.getModifier(stack);
    if (modifier == null) {
      return -1;
    }
    return ARGB.opaque(ResourceColorManager.getColor(Util.makeTranslationKey("modifier", modifier)));
  }

  @Override
  public MapCodec<ModifierCrystalTintSource> type() {
    return MAP_CODEC;
  }
}
