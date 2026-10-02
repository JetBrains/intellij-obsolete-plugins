// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Reports {@code @Singleton} classes that inject {@code @SessionScoped} or
 * {@code @RequestScoped} dependencies. A singleton lives for the entire application
 * lifetime, so directly injecting a narrower-scoped dependency causes it to be
 * captured once and reused incorrectly. Use a {@code Provider} instead.
 *
 * <p>Example:
 * <pre>
 * // Flagged: singleton injects a session-scoped dependency
 * {@literal @}Singleton
 * class AppCache {
 *     {@literal @}Inject UserSession session; // UserSession is @SessionScoped
 * }
 *
 * // OK: use Provider to obtain scoped instances on demand
 * {@literal @}Singleton
 * class AppCache {
 *     {@literal @}Inject Provider&lt;UserSession&gt; sessionProvider;
 * }
 * </pre>
 */
public final class SingletonInjectsScopedInspection extends ScopedInjectionInspectionBase {
  public SingletonInjectsScopedInspection() {
    super(
      GuiceAnnotations.SINGLETONS,
      Set.of(GuiceAnnotations.SESSION_SCOPED, GuiceAnnotations.REQUEST_SCOPED)
    );
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("singleton.injects.scoped.problem.descriptor");
  }
}