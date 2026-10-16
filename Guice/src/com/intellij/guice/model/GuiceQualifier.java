// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The qualifier part of a Guice key, without PSI.
 *
 * <p>Guice treats {@code com.google.inject.name.Named}, {@code javax.inject.Named} and
 * {@code jakarta.inject.Named} as the same annotation, so all three become {@link Named}.
 */
@ApiStatus.Experimental
public sealed interface GuiceQualifier {

  /**
   * A {@code @Named} qualifier.
   *
   * @param value the name, or {@code null} when the name is not a compile-time constant
   */
  record Named(@Nullable String value) implements GuiceQualifier {}

  /**
   * A qualifier given only by its annotation type, e.g. {@code annotatedWith(Db.class)} or {@code @Db}
   * on an annotation without attributes.
   */
  record Marker(@NotNull String fqn) implements GuiceQualifier {}

  /**
   * A qualifier annotation instance with explicit attribute values.
   *
   * @param attributes the attributes as sorted {@code name=value} pairs, separated by {@code ;}
   */
  record Instance(@NotNull String fqn, @NotNull String attributes) implements GuiceQualifier {}

  /**
   * An annotation that does not resolve. It can be a qualifier, so it must not match an unqualified key.
   *
   * @param text the reference text of the annotation or expression
   */
  record Unknown(@NotNull String text) implements GuiceQualifier {}

  /**
   * Tells if two keys with these qualifiers name the same Guice binding. The check is symmetric.
   *
   * <ul>
   *   <li>A {@code @Named} value that is not a constant matches every {@code @Named} value.</li>
   *   <li>A {@link Marker} matches an {@link Instance} of the same type.
   *       Guice uses this fallback when it looks up a key with attributes.</li>
   *   <li>An {@link Unknown} qualifier matches only the same text.</li>
   * </ul>
   */
  static boolean matches(@Nullable GuiceQualifier a, @Nullable GuiceQualifier b) {
    if (a == null || b == null) return a == b;
    if (a instanceof Named na && b instanceof Named nb) {
      return na.value() == null || nb.value() == null || na.value().equals(nb.value());
    }
    String fqnA = typeFqn(a);
    String fqnB = typeFqn(b);
    if (fqnA != null && fqnA.equals(fqnB)) {
      return a instanceof Marker || b instanceof Marker || a.equals(b);
    }
    return a instanceof Unknown && a.equals(b);
  }

  private static @Nullable String typeFqn(@NotNull GuiceQualifier q) {
    if (q instanceof Marker m) return m.fqn();
    if (q instanceof Instance i) return i.fqn();
    return null;
  }
}
