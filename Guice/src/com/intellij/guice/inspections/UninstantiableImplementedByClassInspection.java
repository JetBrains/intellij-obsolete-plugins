// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reports {@code @ImplementedBy} annotations that reference a class Guice cannot
 * instantiate (e.g., an interface, abstract class, or class with no injectable constructor).
 *
 * <p>Example:
 * <pre>
 * // Flagged: AbstractFoo cannot be instantiated by Guice
 * {@literal @}ImplementedBy(AbstractFoo.class)
 * interface Foo {}
 *
 * // OK: FooImpl is concrete with an injectable or no-arg constructor
 * {@literal @}ImplementedBy(FooImpl.class)
 * interface Foo {}
 * </pre>
 */
public final class UninstantiableImplementedByClassInspection extends ClassReferenceAnnotationInspectionBase {
  public UninstantiableImplementedByClassInspection() {
    super(GuiceAnnotations.IMPLEMENTED_BY);
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("uninstantiable.implemented.by.class.problem.descriptor");
  }

  @Override
  protected boolean isValidReferent(@NotNull PsiClass referentClass, @Nullable PsiClass annotatedClass) {
    return GuiceUtils.isInstantiable(referentClass);
  }
}