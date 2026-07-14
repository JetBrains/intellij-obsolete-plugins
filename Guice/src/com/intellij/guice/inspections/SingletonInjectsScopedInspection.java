// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.utils.AnnotationUtils;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor;

import static com.intellij.codeInsight.AnnotationUtil.CHECK_HIERARCHY;

/**
 * Reports {@code @Singleton} classes that inject {@code @SessionScoped} or
 * {@code @RequestScoped} dependencies. A singleton lives for the entire application
 * lifetime, so directly injecting a narrower-scoped dependency causes it to be
 * captured once and reused incorrectly. Use a {@code Provider} instead.
 *
 * <p>Example:
 * <pre>
 * // Flagged: singleton injects a session-scoped dependency
 * {@literal @}Singleton
 * class AppCache {
 *     {@literal @}Inject UserSession session; // UserSession is @SessionScoped
 * }
 *
 * // OK: use Provider to obtain scoped instances on demand
 * {@literal @}Singleton
 * class AppCache {
 *     {@literal @}Inject Provider&lt;UserSession&gt; sessionProvider;
 * }
 * </pre>
 */
public final class SingletonInjectsScopedInspection extends BaseUastInspection {
  public SingletonInjectsScopedInspection() {
    super(UAnnotation.class);
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("singleton.injects.scoped.problem.descriptor");
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
      final String qualifiedName = psiAnnotation.getQualifiedName();
      if (qualifiedName == null || !GuiceAnnotations.INJECTS.contains(qualifiedName)) {
        return true;
      }
      final PsiClass containingClass = PsiTreeUtil.getParentOfType(psiAnnotation, PsiClass.class);
      if (containingClass == null) {
        return true;
      }
      if (!AnnotationUtil.isAnnotated(containingClass, "com.google.inject.Singleton", CHECK_HIERARCHY)) {
        return true;
      }
      final PsiElement owner = psiAnnotation.getParent().getParent();
      if (owner instanceof PsiField field) {
        checkForScopedInjection(field.getTypeElement());
      }
      else if (owner instanceof PsiMethod method) {
        final PsiParameter[] parameters = method.getParameterList().getParameters();
        for (PsiParameter parameter : parameters) {
          checkForScopedInjection(parameter.getTypeElement());
        }
      }
      return true;
    }

    private void checkForScopedInjection(@Nullable PsiTypeElement typeElement) {
      if (typeElement == null) return;
      final PsiType type = typeElement.getType();
      if (!(type instanceof PsiClassType classType)) {
        return;
      }
      final PsiClass referencedClass = classType.resolve();
      if (referencedClass == null) {
        return;
      }
      if (AnnotationUtil.isAnnotated(referencedClass, "com.google.inject.servlet.SessionScoped", CHECK_HIERARCHY) ||
          AnnotationUtil.isAnnotated(referencedClass, "com.google.inject.servlet.RequestScoped", CHECK_HIERARCHY)) {
        registerError(typeElement);
        return;
      }
      PsiModifierList modifierList = referencedClass.getModifierList();
      if (modifierList == null) {
        return;
      }
      final PsiAnnotation implementedByAnnotation = modifierList.findAnnotation(GuiceAnnotations.IMPLEMENTED_BY);
      if (implementedByAnnotation == null) {
        return;
      }
      final PsiElement defaultValue = AnnotationUtils.findDefaultValue(implementedByAnnotation);
      if (defaultValue == null) {
        return;
      }
      if (!(defaultValue instanceof PsiClassObjectAccessExpression)) {
        return;
      }
      final PsiTypeElement classTypeElement = ((PsiClassObjectAccessExpression)defaultValue).getOperand();
      final PsiType implementedByClassType = classTypeElement.getType();
      if (!(implementedByClassType instanceof PsiClassType)) {
        return;
      }
      final PsiClass implementedByClass = ((PsiClassType)implementedByClassType).resolve();
      if (implementedByClass == null) {
        return;
      }
      if (AnnotationUtil.isAnnotated(implementedByClass, "com.google.inject.servlet.SessionScoped", CHECK_HIERARCHY) ||
          AnnotationUtil.isAnnotated(implementedByClass, "com.google.inject.servlet.RequestScoped", CHECK_HIERARCHY)) {
        registerError(typeElement);
      }
    }
  }
}