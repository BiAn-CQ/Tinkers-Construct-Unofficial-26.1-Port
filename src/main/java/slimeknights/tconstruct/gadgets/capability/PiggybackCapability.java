package slimeknights.tconstruct.gadgets.capability;

import net.minecraft.world.entity.player.Player;
import slimeknights.tconstruct.library.tools.capability.TinkerAttachments;

/** Capability logic */
public class PiggybackCapability {
  private PiggybackCapability() {}

  /** Registers this capability */
  public static void register() {
    // Registered centrally through TinkerAttachments.
  }

  public static PiggybackHandler get(Player player) {
    return player.getData(TinkerAttachments.PIGGYBACK);
  }
}
