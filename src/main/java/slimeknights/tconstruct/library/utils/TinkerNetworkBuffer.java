package slimeknights.tconstruct.library.utils;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;

/** Helpers for codecs that require registry-aware network buffers on 26.1. */
public final class TinkerNetworkBuffer {
  private TinkerNetworkBuffer() {}

  /**
   * Gets the registry-aware view of a packet buffer.
   *
   * <p>NeoForge supplies a {@link RegistryFriendlyByteBuf} to normal packet
   * codecs, while Mantle's compatibility interfaces still expose the older
   * {@link FriendlyByteBuf} type. Failing loudly keeps registry-dependent
   * codecs from silently losing registry context.</p>
   */
  public static RegistryFriendlyByteBuf registry(FriendlyByteBuf buffer) {
    if (buffer instanceof RegistryFriendlyByteBuf registryBuffer) {
      return registryBuffer;
    }
    throw new IllegalArgumentException("Registry-aware packet buffer required");
  }
}
