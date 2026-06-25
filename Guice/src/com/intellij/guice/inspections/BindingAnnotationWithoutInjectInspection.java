// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor;

import java.util.Collection;
import java.util.List;

import static com.intellij.codeInsight.AnnotationUtil.CHECK_HIERARCHY;

/**
 * Reports binding annotations (e.g., {@code @Named}, {@code @Qualifier}-annotated annotations)
 * used on fields or method parameters that are not themselves annotated with {@code @Inject}
 * (or {@code @Provides} / {@code @CheckedProvides} for method parameters). Without
 * {@code @Inject}, Guice ignores the binding annotation entirely.
 *
 * <p>Example:
 * <pre>
 * // Flagged: @Named is useless without @Inject on the field
 * {@literal @}Named("db") Connection connection;
 *
 * // OK: field is injected
 * {@literal @}Inject {@literal @}Named("db") Connection connection;
 *
 * // Flagged: parameter qualifier without @Inject on method
 * void configure({@literal @}Named("port") int port) {}
 *
 * // OK: method is annotated with @Provides
 * {@literal @}Provides Widget provide({@literal @}Named("port") int port) { ... }
 * </pre>
 */
public final class BindingAnnotationWithoutInjectInspection extends BaseUastInspection {
  private static final Collection<String> INJECT_OR_PROVIDES =
    List.of(GuiceAnnotations.INJECT, GuiceAnnotations.JAVAX_INJECT, GuiceAnnotations.JAKARTA_INJECT, GuiceAnnotations.THROWING_INJECT, GuiceAnnotations.PROVIDES, GuiceAnnotations.CHECKED_PROVIDES);

  public BindingAnnotationWithoutInjectInspection() {
    super(UAnnotation.class);
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("binding.annotation.without.inject.problem.descriptor");
  }

  @Override
  public @NotNull AbstractUastNonRecursiveVisitor buildUastVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new Visitor(this, holder, isOnTheFly);
  }

  private static class Visitor extends BaseUastInspectionVisitor {
    Visitor(@NotNull BaseUastInspection inspection, @NotNull ProblemsHolder holder, boolean onTheFly) {
      super(inspection, holder, onTheFly);
    }

    @Override
    public boolean visitAnnotation(@NotNull UAnnotation annotation) {
      final PsiElement sourcePsi = annotation.getSourcePsi();
      if (!(sourcePsi instanceof PsiAnnotation psiAnnotation)) {
        return true;
      }
      if (!isBindingAnnotation(psiAnnotation)) {
        return true;
      }
      final PsiVariable boundVariable = PsiTreeUtil.getParentOfType(psiAnnotation, PsiVariable.class);
      if (boundVariable == null) {
        return true;
      }
      if (boundVariable instanceof PsiField) {
        if (!AnnotationUtil.isAnnotated(boundVariable, GuiceAnnotations.INJECTS, CHECK_HIERARCHY)) {
          registerError(psiAnnotation);
        }
      }
      else if (boundVariable instanceof PsiParameter) {
        final PsiMethod containingMethod = PsiTreeUtil.getParentOfType(boundVariable, PsiMethod.class);
        if (containingMethod == null) {
          return true;
        }
        if (!AnnotationUtil.isAnnotated(containingMethod, INJECT_OR_PROVIDES, 0) && !isAssisted(psiAnnotation, containingMethod)) {
          registerError(psiAnnotation);
        }
      }
      return true;
    }

    private static boolean isAssisted(@NotNull PsiAnnotation annotation, @NotNull PsiMethod method) {
      if (!GuiceAnnotations.ASSISTED.equals(annotation.getQualifiedName())) return  false;
      if (method.isConstructor() && AnnotationUtil.isAnnotated(method, GuiceAnnotations.ASSISTED_INJECT, CHECK_HIERARCHY)) return true;
      PsiClass containingClass = method.getContainingClass();

      return containingClass !=null && containingClass.isInterface();
    }
  }

  public static boolean isBindingAnnotation(PsiAnnotation annotation) {
    final PsiJavaCodeReferenceElement referenceElement = annotation.getNameReferenceElement();
    if (referenceElement == null) {
      return false;
    }
    final PsiElement element = referenceElement.resolve();
    if (!(element instanceof PsiClass annotationClass)) {
      return false;
    }
    return AnnotationUtil.isAnnotated(annotationClass, GuiceAnnotations.BINDING_ANNOTATIONS, CHECK_HIERARCHY);
  }
}