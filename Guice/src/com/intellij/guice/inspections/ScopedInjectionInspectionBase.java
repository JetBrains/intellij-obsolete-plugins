// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.utils.AnnotationUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UDeclaration;
import org.jetbrains.uast.UField;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UParameter;
import org.jetbrains.uast.UVariable;
import org.jetbrains.uast.UastUtils;
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor;

import java.util.Collection;

import static com.intellij.codeInsight.AnnotationUtil.CHECK_HIERARCHY;

/**
 * Shared base for inspections that flag narrower-scoped dependencies injected into a broader-scoped class.
 */
abstract class ScopedInjectionInspectionBase extends BaseUastInspection {
  private final @NotNull Collection<String> myOwnerScopes;
  private final @NotNull Collection<String> myForbiddenDependencyScopes;

  ScopedInjectionInspectionBase(@NotNull Collection<String> ownerScopes,
                                @NotNull Collection<String> forbiddenDependencyScopes) {
    super(UAnnotation.class);
    myOwnerScopes = ownerScopes;
    myForbiddenDependencyScopes = forbiddenDependencyScopes;
  }

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
      final String qualifiedName = annotation.getQualifiedName();
      if (qualifiedName == null || !GuiceAnnotations.INJECTS.contains(qualifiedName)) {
        return true;
      }
      final UClass uClass = UastUtils.getParentOfType(annotation, UClass.class);
      if (uClass == null || !AnnotationUtils.isAnnotated(uClass, myOwnerScopes)) {
        return true;
      }
      final UDeclaration owner = AnnotationUtils.resolveAnnotatedDeclaration(annotation);
      if (owner instanceof UField field) {
        checkForScopedInjection(field);
      }
      else if (owner instanceof UMethod method) {
        for (UParameter parameter : method.getUastParameters()) {
          checkForScopedInjection(parameter);
        }
      }
      return true;
    }

    private void checkForScopedInjection(@NotNull UVariable variable) {
      final PsiElement typeAnchor = AnnotationUtils.getTypeAnchor(variable);
      if (typeAnchor == null) return;
      if (!(variable.getType() instanceof PsiClassType classType)) {
        return;
      }
      final PsiClass referencedClass = classType.resolve();
      if (referencedClass == null) {
        return;
      }
      if (AnnotationUtil.isAnnotated(referencedClass, myForbiddenDependencyScopes, CHECK_HIERARCHY)) {
        registerError(typeAnchor);
        return;
      }
      final PsiClass implementedByClass =
        AnnotationUtils.resolveAnnotationClassValue(referencedClass, GuiceAnnotations.IMPLEMENTED_BY);
      if (implementedByClass != null &&
          AnnotationUtil.isAnnotated(implementedByClass, myForbiddenDependencyScopes, CHECK_HIERARCHY)) {
        registerError(typeAnchor);
      }
    }
  }
}
