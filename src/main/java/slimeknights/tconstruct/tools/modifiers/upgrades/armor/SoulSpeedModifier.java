package slimeknights.tconstruct.tools.modifiers.upgrades.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import slimeknights.tconstruct.library.json.LevelingInt;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.tools.modules.armor.SoulSpeedModule;

/** @deprecated use {@link SoulSpeedModule} */
@Deprecated(forRemoval = true)
public class SoulSpeedModifier extends Modifier {
  private final Holder<Enchantment> soulSpeed;

  /**
   * @param soulSpeed holder for {@code minecraft:soul_speed} from the active dynamic enchantment registry
   */
  public SoulSpeedModifier(Holder<Enchantment> soulSpeed) {
    this.soulSpeed = soulSpeed;
  }

  @Override
  protected void registerHooks(Builder hookBuilder) {
    hookBuilder.addModule(new SoulSpeedModule(soulSpeed, LevelingInt.flat(1), ModifierCondition.ANY_TOOL));
  }
}
