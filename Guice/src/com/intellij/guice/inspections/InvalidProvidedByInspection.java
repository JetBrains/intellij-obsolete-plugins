// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reports {@code @ProvidedBy} annotations whose referenced class does not implement
 * {@code Provider} for the annotated type.
 *
 * <p>Example:
 * <pre>
 * // Flagged: WrongProvider provides Bar, not Foo
 * {@literal @}ProvidedBy(WrongProvider.class)
 * interface Foo {}
 *
 * // OK: FooProvider implements Provider&lt;Foo&gt;
 * {@literal @}ProvidedBy(FooProvider.class)
 * interface Foo {}
 * </pre>
 */
public final class InvalidProvidedByInspection extends ClassReferenceAnnotationInspectionBase {
  public InvalidProvidedByInspection() {
    super(GuiceAnnotations.PROVIDED_BY);
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("invalid.provided.by.problem.descriptor");
  }

  @Override
  protected boolean isValidReferent(@NotNull PsiClass referentClass, @Nullable PsiClass annotatedClass) {
    return annotatedClass == null || GuiceUtils.provides(referentClass, annotatedClass);
  }
}