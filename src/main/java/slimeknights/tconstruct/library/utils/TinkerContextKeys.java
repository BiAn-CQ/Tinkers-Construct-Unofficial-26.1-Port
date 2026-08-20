package slimeknights.tconstruct.library.utils;

import net.minecraft.core.RegistryAccess;
import slimeknights.mantle.data.loadable.field.ContextKey;

/** Context keys removed from Mantle's 26.1 public set but still required by Tinkers loaders. */
public final class TinkerContextKeys {
  private TinkerContextKeys() {}

  /** Dynamic registry access for datapack decoding. */
  public static final ContextKey<RegistryAccess> REGISTRY_ACCESS = ContextKey.REGISTRY_ACCESS;
}
