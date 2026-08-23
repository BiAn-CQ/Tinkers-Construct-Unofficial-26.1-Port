package slimeknights.tconstruct.test;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.connection.ConnectionType;

/**
 * Shared fixtures for core tests running through NeoForge's unit-test loader.
 * Tests needing a full game server lifecycle belong in GameTests.
 */
public abstract class CoreTestBootstrap {
  static {
    bindMissingItemComponents();
    bindMissingFluidComponents();
  }

  /**
   * Makes item holders usable by pure unit tests that do not exercise default component values.
   * The normal game/server bootstrap resolves delayed components after tags are loaded; the unit
   * test container intentionally has no tag reload, so evaluating those delayed initializers is
   * invalid. Tests that need real default components belong in the GameTest/server lifecycle.
   */
  public static synchronized void bindMissingItemComponents() {
    BuiltInRegistries.ITEM.listElements().forEach(holder -> {
      if (!holder.areComponentsBound()) {
        holder.bindComponents(DataComponentMap.EMPTY);
      }
    });
  }

  /** Makes fluid holders usable by pure unit tests that construct fluid stacks. */
  public static synchronized void bindMissingFluidComponents() {
    BuiltInRegistries.FLUID.listElements().forEach(holder -> {
      if (!holder.areComponentsBound()) {
        holder.bindComponents(DataComponentMap.EMPTY);
      }
    });
  }

  /** Creates a network buffer backed by the registries loaded for the test mod. */
  protected static RegistryFriendlyByteBuf registryBuffer() {
    RegistryAccess registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    return RegistryFriendlyByteBuf.decorator(registryAccess, ConnectionType.OTHER).apply(Unpooled.buffer());
  }
}
