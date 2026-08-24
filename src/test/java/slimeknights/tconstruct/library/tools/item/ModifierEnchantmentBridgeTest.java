package slimeknights.tconstruct.library.tools.item;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemInstance;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.item.armor.ModifiableArmorItem;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableLauncherItem;
import slimeknights.tconstruct.test.CoreTestBootstrap;

import static org.assertj.core.api.Assertions.assertThat;

class ModifierEnchantmentBridgeTest extends CoreTestBootstrap {
  @Test
  void allModifiableItemTypesOverrideItemInstanceEnchantmentLookup() throws ReflectiveOperationException {
    assertEnchantmentLookupOverride(ModifiableItem.class);
    assertEnchantmentLookupOverride(ModifiableLauncherItem.class);
    assertEnchantmentLookupOverride(ModifiableArmorItem.class);
  }

  private static void assertEnchantmentLookupOverride(Class<?> itemClass) throws NoSuchMethodException {
    assertThat(itemClass.getMethod("getEnchantmentLevel", ItemInstance.class, Holder.class).getDeclaringClass())
      .isEqualTo(itemClass);
  }
}
