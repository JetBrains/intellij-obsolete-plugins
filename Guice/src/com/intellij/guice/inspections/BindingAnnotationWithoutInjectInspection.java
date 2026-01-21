// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

import static com.intellij.codeInsight.AnnotationUtil.CHECK_HIERARCHY;

public final class BindingAnnotationWithoutInjectInspection extends BaseInspection {
  private static final Collection<String> INJECT_OR_PROVIDES =
    List.of(GuiceAnnotations.INJECT, GuiceAnnotations.JAVAX_INJECT, GuiceAnnotations.JAKARTA_INJECT, GuiceAnnotations.PROVIDES);

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("binding.annotation.without.inject.problem.descriptor");
  }

  @Override
  public BaseInspectionVisitor buildVisitor() {
    return new Visitor();
  }

  private static class Visitor extends BaseInspectionVisitor {
    @Override
    public void visitAnnotation(@NotNull PsiAnnotation annotation) {
      super.visitAnnotation(annotation);
      if (!isBindingAnnotation(annotation)) {
        return;
      }
      final PsiVariable boundVariable = PsiTreeUtil.getParentOfType(annotation, PsiVariable.class);
      if (boundVariable == null) {
        return;
      }
      if (boundVariable instanceof PsiField) {
        if (!AnnotationUtil.isAnnotated(boundVariable, GuiceAnnotations.INJECTS, CHECK_HIERARCHY)) {
          registerError(annotation);
        }
      }
      else if (boundVariable instanceof PsiParameter) {
        final PsiMethod containingMethod = PsiTreeUtil.getParentOfType(boundVariable, PsiMethod.class);
        if (containingMethod == null) {
          return;
        }
        if (!AnnotationUtil.isAnnotated(containingMethod, INJECT_OR_PROVIDES, 0) && !isAssisted(annotation, containingMethod)) {
          registerError(annotation);
        }
      }
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
    return AnnotationUtil.isAnnotated(annotationClass, GuiceAnnotations.BINDING_ANNOTATION, CHECK_HIERARCHY);
  }
}