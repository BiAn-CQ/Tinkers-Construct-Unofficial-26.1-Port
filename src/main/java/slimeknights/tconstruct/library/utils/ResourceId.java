package slimeknights.tconstruct.library.utils;

import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Helper for use with our extensions of resource location for some type safety in IDs.
 * Note we left {@link Identifier#withPath(String)} and alike as returning {@link Identifier} as there is not much use extending an ID.
 * @see IdParser
 */
public abstract class ResourceId implements Comparable<ResourceId> {
  private final Identifier location;

  protected static final class Dummy {}

  protected ResourceId(String namespace, String path, @Nullable Dummy pDummy) {
    this.location = Identifier.fromNamespaceAndPath(namespace, path);
  }

  public ResourceId(Identifier location) {
    this.location = location;
  }

  public ResourceId(String namespace, String path) {
    this.location = Identifier.fromNamespaceAndPath(namespace, path);
  }

  public ResourceId(String location) {
    this.location = Identifier.parse(location);
  }

  /** Gets the wrapped vanilla resource location. */
  public Identifier location() {
    return location;
  }

  public String getNamespace() {
    return location.getNamespace();
  }

  public String getPath() {
    return location.getPath();
  }

  public Identifier withPath(String path) {
    return location.withPath(path);
  }

  public Identifier withSuffix(String suffix) {
    return location.withSuffix(suffix);
  }

  public Identifier withPrefix(String prefix) {
    return location.withPrefix(prefix);
  }

  @Override
  public String toString() {
    return location.toString();
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object instanceof ResourceId other) {
      return location.equals(other.location);
    }
    return location.equals(object);
  }

  @Override
  public int hashCode() {
    return Objects.hash(location);
  }

  @Override
  public int compareTo(ResourceId other) {
    return location.compareTo(other.location);
  }

  /* Helpers for static constructors */

  /**
   * Creates a new ID from the given string
   * @param string  String
   * @return  ID, or null if invalid
   */
  @Nullable
  protected static <T extends ResourceId> T tryParse(String string, BiFunction<String,String,T> constructor) {
    String[] parts = IdParser.decompose(Identifier.DEFAULT_NAMESPACE, string, ':');
    return tryBuild(parts[0], parts[1], constructor);
  }

  /**
   * Creates a new ID from the given namespace and path
   * @param namespace  Namespace
   * @param path       Path
   * @return  ID, or null if invalid
   */
  @Nullable
  protected static <T extends ResourceId> T tryBuild(String namespace, String path, BiFunction<String,String,T> constructor) {
    if (Identifier.isValidNamespace(namespace) && Identifier.isValidPath(path)) {
      return constructor.apply(namespace, path);
    }
    return null;
  }
}
