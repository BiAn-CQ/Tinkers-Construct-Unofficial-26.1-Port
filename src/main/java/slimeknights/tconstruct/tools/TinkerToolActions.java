package slimeknights.tconstruct.tools;

import net.neoforged.neoforge.common.ItemAbility;

/** Custom tool actions defined by the mod */
public class TinkerToolActions {
  /** Legacy mining abilities removed from NeoForge's built-in constants in 26.1.
   *  Tinkers keeps the stable identifiers for tool-definition and addon compatibility;
   *  actual harvest behavior is driven by the tool definition's harvest modules. */
  public static final ItemAbility PICKAXE_DIG = ItemAbility.get("pickaxe_dig");
  public static final ItemAbility AXE_DIG = ItemAbility.get("axe_dig");
  public static final ItemAbility SHOVEL_DIG = ItemAbility.get("shovel_dig");
  public static final ItemAbility HOE_DIG = ItemAbility.get("hoe_dig");
  public static final ItemAbility SWORD_DIG = ItemAbility.get("sword_dig");

  /** Items that should receive shield protection enchantment attributes. */
  public static final ItemAbility SHIELD_BLOCK = ItemAbility.get("shield_block");
  /** Tinker tools that can disable shields on attack */
  public static final ItemAbility SHIELD_DISABLE = ItemAbility.get("shield_disable");
  /** Fishing rods that can act as a grappling hook */
  public static final ItemAbility GRAPPLE_HOOK = ItemAbility.get("grapple_hook");
  /** Makes the tool use the drill attack during its dash action */
  public static final ItemAbility DRILL_ATTACK = ItemAbility.get("drill_attack");
  /** Fishing rods that can collect items */
  public static final ItemAbility ITEM_HOOK = ItemAbility.get("item_hook");
}
