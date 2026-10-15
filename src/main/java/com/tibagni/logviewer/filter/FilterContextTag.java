package com.tibagni.logviewer.filter;

import com.tibagni.logviewer.util.StringUtils;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A unique tag identifying an execution context for {@link Filter} instances.
 * Duplicate registrations with the same identifier will throw {@link IllegalArgumentException}
 * to prevent unintentional context collisions between components.
 */
public final class FilterContextTag {
  private static final Set<String> REGISTERED_TAGS = ConcurrentHashMap.newKeySet();

  private final String id;

  private FilterContextTag(String id) {
    this.id = id;
  }

  /**
   * Registers a new unique FilterContextTag. Throws IllegalArgumentException if
   * this tag ID has already been registered.
   */
  public static FilterContextTag register(String id) {
    if (StringUtils.isEmpty(id)) {
      throw new IllegalArgumentException("FilterContextTag ID must not be null or empty");
    }
    if (!REGISTERED_TAGS.add(id)) {
      throw new IllegalArgumentException("FilterContextTag '" + id + "' is already registered!");
    }
    return new FilterContextTag(id);
  }

  public String getId() {
    return id;
  }

  @Override
  public String toString() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FilterContextTag that = (FilterContextTag) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @VisibleForTesting
  public static void resetRegistryForTesting() {
    REGISTERED_TAGS.clear();
  }

  @VisibleForTesting
  public static void unregisterForTesting(String id) {
    REGISTERED_TAGS.remove(id);
  }
}
