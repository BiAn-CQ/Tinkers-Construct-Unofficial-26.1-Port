package slimeknights.tconstruct.fixture;

import net.minecraft.resources.Identifier;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry;

/** Helpers for generic registration tasks */
public class RegistrationFixture {
  /** Registers an object to a registry without risk of tests failing if its registered already */
  public static <T> void register(GenericLoaderRegistry<? super T> registry, String name, RecordLoadable<T> value) {
    try {
      registry.register(Identifier.fromNamespaceAndPath("test", name), value);
    } catch (Exception e) {
      // no-op
    }
  }
}
