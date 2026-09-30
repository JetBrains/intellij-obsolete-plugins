// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Reports {@code @SessionScoped} classes that inject {@code @RequestScoped} dependencies.
 * A session-scoped object outlives any single request, so directly injecting a
 * request-scoped dependency leads to stale or incorrect data. Use a {@code Provider}
 * instead to obtain a fresh request-scoped instance per request.
 *
 * <p>Example:
 * <pre>
 * // Flagged: session-scoped class injects request-scoped dependency
 * {@literal @}SessionScoped
 * class UserPreferences {
 *     {@literal @}Inject RequestData requestData; // RequestData is @RequestScoped
 * }
 *
 * // OK: use Provider to avoid scope mismatch
 * {@literal @}SessionScoped
 * class UserPreferences {
 *     {@literal @}Inject Provider&lt;RequestData&gt; requestDataProvider;
 * }
 * </pre>
 */
public final class SessionScopedInjectsRequestScopedInspection extends ScopedInjectionInspectionBase {
  public SessionScopedInjectsRequestScopedInspection() {
    super(
      Set.of(GuiceAnnotations.SESSION_SCOPED),
      Set.of(GuiceAnnotations.REQUEST_SCOPED)
    );
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("session.scoped.injects.request.scoped.problem.descriptor");
  }
}