package slimeknights.tconstruct.tools.logic;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import slimeknights.tconstruct.tools.entity.ModifiableArrow;

/** Dispenser behavior for a modifiable arrow item */
public class ModifiableArrowDispenserBehavior extends DefaultDispenseItemBehavior {
  public static final ModifiableArrowDispenserBehavior INSTANCE = new ModifiableArrowDispenserBehavior();

  private ModifiableArrowDispenserBehavior() {}

  private static Projectile getProjectile(Level level, Position position, ItemStack stack) {
    ModifiableArrow arrow = new ModifiableArrow(level, position.x(), position.y(), position.z());
    arrow.onCreate(stack, null);
    arrow.pickup = Pickup.ALLOWED;
    return arrow;
  }

  @Override
  protected ItemStack execute(BlockSource source, ItemStack stack) {
    Level level = source.level();
    Direction direction = source.state().getValue(DispenserBlock.FACING);
    Position position = DispenserBlock.getDispensePosition(source, 0.7, new net.minecraft.world.phys.Vec3(0, 0.1, 0));
    Projectile projectile = getProjectile(level, position, stack);
    projectile.shoot(direction.getStepX(), direction.getStepY(), direction.getStepZ(), 1.1f, 6.0f);
    level.addFreshEntity(projectile);
    stack.shrink(1);
    return stack;
  }
}
