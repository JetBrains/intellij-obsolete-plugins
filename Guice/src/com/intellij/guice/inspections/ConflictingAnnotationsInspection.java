// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierListOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor;

import static com.intellij.codeInsight.AnnotationUtil.CHECK_HIERARCHY;

/**
 * Reports conflicting Guice annotations on the same class or element. The following
 * combinations are flagged as mutually exclusive:
 * <ul>
 *   <li>{@code @ImplementedBy} and {@code @ProvidedBy}</li>
 *   <li>{@code @Singleton} with {@code @SessionScoped} or {@code @RequestScoped}</li>
 *   <li>{@code @SessionScoped} with {@code @RequestScoped}</li>
 * </ul>
 *
 * <p>Example:
 * <pre>
 * // Flagged: conflicting default-binding annotations
 * {@literal @}ImplementedBy(FooImpl.class)
 * {@literal @}ProvidedBy(FooProvider.class)
 * interface Foo {}
 *
 * // Flagged: conflicting scope annotations
 * {@literal @}Singleton
 * {@literal @}RequestScoped
 * class Bar {}
 * </pre>
 */
public final class ConflictingAnnotationsInspection extends BaseUastInspection {
  public ConflictingAnnotationsInspection() {
    super(UAnnotation.class);
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("conflicting.annotations.problem.descriptor");
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
      final PsiElement grandParent = psiAnnotation.getParent().getParent();
      if (!(grandParent instanceof PsiModifierListOwner owner)) {
        return true;
      }
      final String qualifiedName = psiAnnotation.getQualifiedName();
      if (GuiceAnnotations.IMPLEMENTED_BY.equals(qualifiedName)) {
        if (AnnotationUtil.isAnnotated(owner, GuiceAnnotations.PROVIDED_BY, CHECK_HIERARCHY)) {
          registerError(psiAnnotation);
        }
        return true;
      }
      if (GuiceAnnotations.PROVIDED_BY.equals(qualifiedName)) {
        if (AnnotationUtil.isAnnotated(owner, GuiceAnnotations.IMPLEMENTED_BY, CHECK_HIERARCHY)) {
          registerError(psiAnnotation);
        }
        return true;
      }
      if ("com.google.inject.Singleton".equals(qualifiedName)) {
        if (AnnotationUtil.isAnnotated(owner, "com.google.inject.servlet.SessionScoped", CHECK_HIERARCHY) ||
            AnnotationUtil.isAnnotated(owner, "com.google.inject.servlet.RequestScoped", CHECK_HIERARCHY)) {
          registerError(psiAnnotation);
        }
        return true;
      }
      if ("com.google.inject.servlet.SessionScoped".equals(qualifiedName)) {
        if (AnnotationUtil.isAnnotated(owner, "com.google.inject.Singleton", CHECK_HIERARCHY) ||
            AnnotationUtil.isAnnotated(owner, "com.google.inject.servlet.RequestScoped", CHECK_HIERARCHY)) {
          registerError(psiAnnotation);
        }
        return true;
      }
      if ("com.google.inject.servlet.RequestScoped".equals(qualifiedName)) {
        if (AnnotationUtil.isAnnotated(owner, "com.google.inject.servlet.SessionScoped", CHECK_HIERARCHY) ||
            AnnotationUtil.isAnnotated(owner, "com.google.inject.Singleton", CHECK_HIERARCHY)) {
          registerError(psiAnnotation);
        }
      }
      return true;
    }
  }
}