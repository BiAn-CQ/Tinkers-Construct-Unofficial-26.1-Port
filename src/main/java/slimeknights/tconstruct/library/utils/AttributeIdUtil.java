package slimeknights.tconstruct.library.utils;

import net.minecraft.resources.Identifier;
import slimeknights.tconstruct.TConstruct;

import java.util.UUID;

/** Preserves the identity of pre-1.21 UUID attribute modifiers in the Identifier-based API. */
public final class AttributeIdUtil {
  private AttributeIdUtil() {}

  public static Identifier fromLegacyUuid(UUID uuid) {
    return TConstruct.getResource("legacy_attribute/" + uuid);
  }
}
