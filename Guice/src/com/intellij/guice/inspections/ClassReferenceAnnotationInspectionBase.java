// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.guice.utils.AnnotationUtils;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UastUtils;
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor;

/**
 * Shared base for inspections that validate the default class attribute of {@code @ImplementedBy}
 * and {@code @ProvidedBy} annotations.
 */
abstract class ClassReferenceAnnotationInspectionBase extends BaseUastInspection {
  private final @NotNull String myAnnotationFqn;

  ClassReferenceAnnotationInspectionBase(@NotNull String annotationFqn) {
    super(UAnnotation.class);
    myAnnotationFqn = annotationFqn;
  }

  protected abstract boolean isValidReferent(@NotNull PsiClass referentClass, @Nullable PsiClass annotatedClass);

  @Override
  public @NotNull AbstractUastNonRecursiveVisitor buildUastVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new Visitor(this, holder, isOnTheFly);
  }

  private final class Visitor extends BaseUastInspectionVisitor {
    Visitor(@NotNull BaseUastInspection inspection, @NotNull ProblemsHolder holder, boolean onTheFly) {
      super(inspection, holder, onTheFly);
    }

    @Override
    public boolean visitAnnotation(@NotNull UAnnotation annotation) {
      if (!myAnnotationFqn.equals(annotation.getQualifiedName())) {
        return true;
      }
      final UExpression defaultValue = AnnotationUtils.findDefaultValue(annotation);
      if (defaultValue == null) {
        return true;
      }
      final PsiClass referentClass = GuiceUtils.resolveClass(GuiceUtils.getBindingTypeFromExpression(defaultValue));
      if (referentClass == null) {
        return true;
      }
      final UClass uClass = UastUtils.getParentOfType(annotation, UClass.class);
      final PsiClass annotatedClass = uClass != null ? uClass.getJavaPsi() : null;
      if (!isValidReferent(referentClass, annotatedClass)) {
        registerClassLiteralError(defaultValue);
      }
      return true;
    }
  }
}
