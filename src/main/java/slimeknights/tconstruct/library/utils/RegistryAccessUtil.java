package slimeknights.tconstruct.library.utils;

import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import slimeknights.mantle.client.SafeClientAccess;

/** Provides the active registry lookup for component-aware runtime serialization. */
public final class RegistryAccessUtil {
  private RegistryAccessUtil() {}

  /**
   * Gets the active server or client registry access.
   *
   * @throws IllegalStateException if called before a world has been created
   */
  public static HolderLookup.Provider getRegistryAccess() {
    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
    if (server != null) {
      return server.registryAccess();
    }
    Level level = SafeClientAccess.getLevel();
    if (level != null) {
      return level.registryAccess();
    }
    throw new IllegalStateException("Component serialization requires an active level registry access");
  }
}
