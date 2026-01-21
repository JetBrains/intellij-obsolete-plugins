// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierListOwner;
import org.jetbrains.annotations.NotNull;

import static com.intellij.codeInsight.AnnotationUtil.CHECK_HIERARCHY;

public final class ConflictingAnnotationsInspection extends BaseInspection {
  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("conflicting.annotations.problem.descriptor");
  }

  @Override
  public BaseInspectionVisitor buildVisitor() {
    return new Visitor();
  }

  private static class Visitor extends BaseInspectionVisitor {
    @Override
    public void visitAnnotation(@NotNull PsiAnnotation annotation) {
      super.visitAnnotation(annotation);
      final PsiElement grandParent = annotation.getParent().getParent();
      if (!(grandParent instanceof PsiModifierListOwner owner)) {
        return;
      }
      final String qualifiedName = annotation.getQualifiedName();
      if (GuiceAnnotations.IMPLEMENTED_BY.equals(qualifiedName)) {
        if (AnnotationUtil.isAnnotated(owner, GuiceAnnotations.PROVIDED_BY, CHECK_HIERARCHY)) {
          registerError(annotation);
        }
        return;
      }
      if (GuiceAnnotations.PROVIDED_BY.equals(qualifiedName)) {
        if (AnnotationUtil.isAnnotated(owner, GuiceAnnotations.IMPLEMENTED_BY, CHECK_HIERARCHY)) {
          registerError(annotation);
        }
        return;
      }
      if ("com.google.inject.Singleton".equals(qualifiedName)) {
        if (AnnotationUtil.isAnnotated(owner, "com.google.inject.servlet.SessionScoped", CHECK_HIERARCHY) ||
            AnnotationUtil.isAnnotated(owner, "com.google.inject.servlet.RequestScoped", CHECK_HIERARCHY)) {
          registerError(annotation);
        }
        return;
      }
      if ("com.google.inject.servlet.SessionScoped".equals(qualifiedName)) {
        if (AnnotationUtil.isAnnotated(owner, "com.google.inject.Singleton", CHECK_HIERARCHY) ||
            AnnotationUtil.isAnnotated(owner, "com.google.inject.servlet.RequestScoped", CHECK_HIERARCHY)) {
          registerError(annotation);
        }
        return;
      }
      if ("com.google.inject.servlet.RequestScoped".equals(qualifiedName)) {
        if (AnnotationUtil.isAnnotated(owner, "com.google.inject.servlet.SessionScoped", CHECK_HIERARCHY) ||
            AnnotationUtil.isAnnotated(owner, "com.google.inject.Singleton", CHECK_HIERARCHY)) {
          registerError(annotation);
        }
      }
    }
  }
}