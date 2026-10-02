// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reports {@code @ProvidedBy} annotations that reference a provider class Guice cannot
 * instantiate (e.g., an interface, abstract class, or class with no injectable constructor).
 *
 * <p>Example:
 * <pre>
 * // Flagged: AbstractFooProvider is abstract and cannot be instantiated
 * {@literal @}ProvidedBy(AbstractFooProvider.class)
 * interface Foo {}
 *
 * // OK: FooProvider is a concrete class
 * {@literal @}ProvidedBy(FooProvider.class)
 * interface Foo {}
 * </pre>
 */
public final class UninstantiableProvidedByClassInspection extends ClassReferenceAnnotationInspectionBase {
  public UninstantiableProvidedByClassInspection() {
    super(GuiceAnnotations.PROVIDED_BY);
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("uninstantiable.provided.by.class.problem.descriptor");
  }

  @Override
  protected boolean isValidReferent(@NotNull PsiClass referentClass, @Nullable PsiClass annotatedClass) {
    return GuiceUtils.isInstantiable(referentClass);
  }
}