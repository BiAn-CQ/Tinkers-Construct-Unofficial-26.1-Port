package slimeknights.tconstruct.library.tools.helper;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Weapon;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.test.CoreTestBootstrap;

import static org.assertj.core.api.Assertions.assertThat;

class ModifierUtilComponentTest extends CoreTestBootstrap {
  @Test
  void shieldDisableActionMirrorsToWeaponComponent() {
    ItemStack stack = new ItemStack(Items.STICK);

    ModifierUtil.updateShieldDisableComponent(stack, true);

    Weapon weapon = stack.get(DataComponents.WEAPON);
    assertThat(weapon).isNotNull();
    assertThat(weapon.itemDamagePerAttack()).isZero();
    assertThat(weapon.disableBlockingForSeconds()).isEqualTo(Weapon.AXE_DISABLES_BLOCKING_FOR_SECONDS);
  }

  @Test
  void removingShieldDisableActionClearsOnlyTinkersMirror() {
    ItemStack stack = new ItemStack(Items.STICK);
    ModifierUtil.updateShieldDisableComponent(stack, true);

    ModifierUtil.updateShieldDisableComponent(stack, false);

    assertThat(stack.has(DataComponents.WEAPON)).isFalse();
  }

  @Test
  void unrelatedWeaponComponentIsPreserved() {
    ItemStack stack = new ItemStack(Items.STICK);
    Weapon custom = new Weapon(2, 1.5F);
    stack.set(DataComponents.WEAPON, custom);

    ModifierUtil.updateShieldDisableComponent(stack, false);

    assertThat(stack.get(DataComponents.WEAPON)).isEqualTo(custom);
  }
}
