// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

/**
 * The role of a {@link GuiceEntry} in the Guice binding graph.
 *
 * <p>Navigation is always between entries with <b>opposite</b> roles:
 * injection points navigate to binding sites, and vice versa.
 */
public enum EntryRole {
  /**
   * An element that <b>consumes</b> a binding: {@code @Inject} fields, {@code @Inject}
   * constructor/method parameters, {@code .getProvider()} calls, and the {@code .to()}/
   * {@code .toProvider()} implementation references.
   */
  INJECTION_POINT,

  /**
   * An element that <b>provides</b> a binding: {@code bind().to()} chains,
   * {@code @Provides} methods, {@code @Inject} constructors (JIT bindings),
   * and multibinder contributions ({@code addBinding().to()}, {@code @ProvidesIntoSet}).
   */
  BINDING_SITE
}
