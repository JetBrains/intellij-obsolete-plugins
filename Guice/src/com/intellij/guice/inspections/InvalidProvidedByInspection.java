// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.utils.AnnotationUtils;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor;

/**
 * Reports {@code @ProvidedBy} annotations where the referenced class does not actually
 * provide the annotated type (i.e., does not implement {@code Provider<AnnotatedType>}).
 *
 * <p>Example:
 * <pre>
 * // Flagged: WrongProvider does not implement Provider&lt;Foo&gt;
 * {@literal @}ProvidedBy(WrongProvider.class)
 * class Foo {}
 *
 * // OK: FooProvider implements Provider&lt;Foo&gt;
 * {@literal @}ProvidedBy(FooProvider.class)
 * class Foo {}
 * class FooProvider implements Provider&lt;Foo&gt; { ... }
 * </pre>
 */
public final class InvalidProvidedByInspection extends BaseUastInspection {
  public InvalidProvidedByInspection() {
    super(UAnnotation.class);
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("invalid.provided.by.problem.descriptor");
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
      final String qualifiedName = annotation.getQualifiedName();
      if (!GuiceAnnotations.PROVIDED_BY.equals(qualifiedName)) {
        return true;
      }
      final PsiElement sourcePsi = annotation.getSourcePsi();
      if (!(sourcePsi instanceof PsiAnnotation psiAnnotation)) {
        return true;
      }
      final PsiClass containingClass = PsiTreeUtil.getParentOfType(psiAnnotation, PsiClass.class);
      if (containingClass == null) {
        return true;
      }
      final PsiElement defaultValue = AnnotationUtils.findDefaultValue(psiAnnotation);
      if (defaultValue == null) {
        return true;
      }
      if (!(defaultValue instanceof PsiClassObjectAccessExpression)) {
        return true;
      }
      final PsiTypeElement classTypeElement = ((PsiClassObjectAccessExpression)defaultValue).getOperand();
      final PsiType classType = classTypeElement.getType();
      if (!(classType instanceof PsiClassType)) {
        return true;
      }
      final PsiClass referentClass = ((PsiClassType)classType).resolve();
      if (referentClass == null) {
        return true;
      }
      if (GuiceUtils.provides(referentClass, containingClass)) {
        return true;
      }
      registerError(classTypeElement);
      return true;
    }
  }
}