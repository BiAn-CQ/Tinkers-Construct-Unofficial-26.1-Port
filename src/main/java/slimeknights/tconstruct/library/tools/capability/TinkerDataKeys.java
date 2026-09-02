package slimeknights.tconstruct.library.tools.capability;

import slimeknights.mantle.data.registry.IdAwareComponentRegistry;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.data.FloatMultiplier;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.ComputableDataKey;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.TinkerDataKey;

/** All tinker data keys intended to be used by multiple modifiers */
public interface TinkerDataKeys {
  IdAwareComponentRegistry<TinkerDataKey<Integer>> INTEGER_REGISTRY = new IdAwareComponentRegistry<>("Unknown data key");

  static void init() {}

  /** If this key is greater than 0, the offhand will be rendered even if empty */
  TinkerDataKey<Integer> SHOW_EMPTY_OFFHAND = TConstruct.createKey("show_empty_offhand"); // unregistered as ShowOffhandModule exists

  /** Float value for the FOV modifier, will be 1.0 if no change */
  ComputableDataKey<FloatMultiplier> FOV_MODIFIER = TConstruct.createKey("zoom_multiplier", FloatMultiplier::new);

  /** FOV modifier that only applies when not disabled in the settings menu */
  ComputableDataKey<FloatMultiplier> SCALED_FOV_MODIFIER = TConstruct.createKey("scaled_fov_multiplier", FloatMultiplier::new);

  /** Crystalstrike level for knockback restriction */
  TinkerDataKey<Integer> CRYSTALSTRIKE = intKey("crystalstrike_knockback");

  /** Soul belt level for hotbar preservation */
  TinkerDataKey<Integer> SOUL_BELT = intKey("soul_belt");
  /** Levels of magnetic on the tool */
  TinkerDataKey<Integer> MAGNET = intKey("magnet");


  /** Creates and registers an integer key */
  private static TinkerDataKey<Integer> intKey(String name) {
    return INTEGER_REGISTRY.register(TConstruct.createKey(name));
  }
}
