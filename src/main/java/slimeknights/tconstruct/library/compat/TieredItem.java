package slimeknights.tconstruct.library.compat;

import net.minecraft.world.item.Item;

/** Compatibility base for items that still expose a legacy mining tier. */
public class TieredItem extends Item {
  private final Tier tier;

  public TieredItem(Tier tier, Properties properties) {
    super(properties);
    this.tier = tier;
  }

  public Tier getTier() {
    return tier;
  }
}
