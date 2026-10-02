// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.psi.PsiClass;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reports {@code @ImplementedBy} annotations whose referenced class does not actually
 * implement or extend the annotated type.
 *
 * <p>Example:
 * <pre>
 * // Flagged: Unrelated does not implement Service
 * {@literal @}ImplementedBy(Unrelated.class)
 * interface Service {}
 *
 * // OK: ServiceImpl implements Service
 * {@literal @}ImplementedBy(ServiceImpl.class)
 * interface Service {}
 * </pre>
 */
public final class InvalidImplementedByInspection extends ClassReferenceAnnotationInspectionBase {
  public InvalidImplementedByInspection() {
    super(GuiceAnnotations.IMPLEMENTED_BY);
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("invalid.implemented.by.problem.descriptor");
  }

  @Override
  protected boolean isValidReferent(@NotNull PsiClass referentClass, @Nullable PsiClass annotatedClass) {
    return annotatedClass == null || InheritanceUtil.isInheritorOrSelf(referentClass, annotatedClass, true);
  }
}
