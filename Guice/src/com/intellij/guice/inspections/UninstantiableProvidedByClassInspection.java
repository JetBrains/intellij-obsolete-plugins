// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.utils.AnnotationUtils;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor;

/**
 * Reports {@code @ProvidedBy} annotations where the referenced provider class cannot
 * be instantiated by Guice (e.g., it is abstract, an interface, or has no suitable
 * constructor).
 *
 * <p>Example:
 * <pre>
 * // Flagged: AbstractProvider cannot be instantiated
 * {@literal @}ProvidedBy(AbstractProvider.class)
 * class Foo {}
 * abstract class AbstractProvider implements Provider&lt;Foo&gt; {}
 *
 * // OK: ConcreteProvider can be instantiated
 * {@literal @}ProvidedBy(ConcreteProvider.class)
 * class Foo {}
 * class ConcreteProvider implements Provider&lt;Foo&gt; { ... }
 * </pre>
 */
public final class UninstantiableProvidedByClassInspection extends BaseUastInspection {
  public UninstantiableProvidedByClassInspection() {
    super(UAnnotation.class);
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("uninstantiable.provided.by.class.problem.descriptor");
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
      if (GuiceUtils.isInstantiable(referentClass)) {
        return true;
      }
      registerError(classTypeElement);
      return true;
    }
  }
}