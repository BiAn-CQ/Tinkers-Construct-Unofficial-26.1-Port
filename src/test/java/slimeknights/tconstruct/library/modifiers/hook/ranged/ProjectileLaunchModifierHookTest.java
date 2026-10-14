package slimeknights.tconstruct.library.modifiers.hook.ranged;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.test.CoreTestBootstrap;

import static org.mockito.Mockito.*;

class ProjectileLaunchModifierHookTest extends CoreTestBootstrap {
  @Test
  void shootPassesTheOriginalAmmoToTheLaunchHook() {
    var hook = mock(ProjectileLaunchModifierHook.class, CALLS_REAL_METHODS);
    var tool = mock(IToolStackView.class);
    var modifier = mock(ModifierEntry.class);
    var shooter = mock(LivingEntity.class);
    var projectile = mock(Projectile.class);
    var data = new ModDataNBT();
    var ammo = new ItemStack(Items.ARROW, 3);
    doNothing().when(hook).onProjectileLaunch(tool, modifier, shooter, ammo, projectile, null, data, true);

    hook.onProjectileShoot(tool, modifier, shooter, ammo, projectile, null, data, true);

    verify(hook).onProjectileLaunch(tool, modifier, shooter, ammo, projectile, null, data, true);
  }

  @Test
  @SuppressWarnings("deprecation")
  void legacyLaunchImplementationsStillReceiveTheShootEvent() {
    var hook = mock(ProjectileLaunchModifierHook.class, CALLS_REAL_METHODS);
    var tool = mock(IToolStackView.class);
    var modifier = mock(ModifierEntry.class);
    var shooter = mock(LivingEntity.class);
    var projectile = mock(Projectile.class);
    var data = new ModDataNBT();

    hook.onProjectileShoot(tool, modifier, shooter, new ItemStack(Items.ARROW), projectile, null, data, false);

    verify(hook).onProjectileLaunch(tool, modifier, shooter, projectile, null, data, false);
  }
}
