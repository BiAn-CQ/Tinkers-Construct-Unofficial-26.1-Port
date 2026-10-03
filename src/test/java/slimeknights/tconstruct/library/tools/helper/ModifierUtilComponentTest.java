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
  @Test
  void blockingComponentEnablesNativeBlockingAndClearsOnRelease() {
    var base = new net.minecraft.world.item.component.BlocksAttacks(0.25f, 1,
      java.util.List.of(new net.minecraft.world.item.component.BlocksAttacks.DamageReduction(90, java.util.Optional.empty(), 0, 1)),
      new net.minecraft.world.item.component.BlocksAttacks.ItemDamageFunction(3, 1, 1),
      java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty());
    ItemStack stack = new ItemStack(Items.STICK);
    ModifierUtil.updateShieldBlockingComponent(stack, true, base);
    var component = stack.get(DataComponents.BLOCKS_ATTACKS);
    assertThat(component).isNotNull();
    assertThat(component.blockDelayTicks()).isEqualTo(5);
    assertThat(component.itemDamage().apply(20)).isZero();
    var user = org.mockito.Mockito.mock(net.minecraft.world.entity.LivingEntity.class);
    org.mockito.Mockito.when(user.isUsingItem()).thenReturn(true);
    org.mockito.Mockito.doCallRealMethod().when(user).getItemBlockingWith();
    // Vanilla reads these fields directly when deciding whether an item blocks.
    try {
      var useItem = net.minecraft.world.entity.LivingEntity.class.getDeclaredField("useItem");
      var remaining = net.minecraft.world.entity.LivingEntity.class.getDeclaredField("useItemRemaining");
      useItem.setAccessible(true);
      remaining.setAccessible(true);
      useItem.set(user, stack);
      remaining.setInt(user, -5);
      assertThat(user.getItemBlockingWith()).isSameAs(stack);
      ModifierUtil.updateShieldBlockingComponent(stack, false, base);
      assertThat(user.getItemBlockingWith()).isNull();
    } catch (ReflectiveOperationException exception) {
      throw new AssertionError(exception);
    }
  }
}
