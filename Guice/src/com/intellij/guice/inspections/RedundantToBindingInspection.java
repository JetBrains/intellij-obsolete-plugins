// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.extensions.GuiceCallPattern;
import com.intellij.guice.utils.AnnotationUtils;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UCallExpression;


/**
 * Reports redundant {@code .to()} bindings where the target class is the same as the bound
 * class (a self-binding), or where the target matches the class already specified by an
 * {@code @ImplementedBy} annotation on the bound type.
 */
public final class RedundantToBindingInspection extends BaseUastInspection {
  public RedundantToBindingInspection() {
    extendCall(
        GuiceCallPattern.named("to").requireValueOrTypeArgument(),
        RedundantToBindingInspection::checkToCall
    );
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("redundant.to.binding.problem.descriptor", infos);
  }

  @Override
  public LocalQuickFix buildFix() {
    return new DeleteBindingFix(DeleteBindingFix.Mode.CHAIN_CALL, "remove.binding.target");
  }

  private static void checkToCall(@NotNull UCallExpression expression, @NotNull BaseUastInspectionVisitor visitor) {
    // to(Key.get(T, qualifier)) links to another binding of T; Guice constructs nothing here.
    if (GuiceUtils.linksToQualifiedKey(expression)) {
      return;
    }
    PsiClass referentClass = GuiceUtils.resolveClassArgument(expression);
    if (referentClass == null) {
      return;
    }
    // Only a bind() chain for an unqualified key can be redundant: a multibinder element binds no key of its own,
    // and a qualified key is a different key than the type with the @ImplementedBy annotation.
    if (!GuiceUtils.isStandardBindChain(expression) || GuiceUtils.isQualifiedBindChain(expression)) {
      return;
    }
    final PsiClass boundClass = GuiceUtils.findImplementedClassForBinding(expression);
    if (boundClass == null) {
      return;
    }
    // Guice reads @ProvidedBy and @ImplementedBy on the bound type only, not on its supertypes.
    if (AnnotationUtil.isAnnotated(boundClass, GuiceAnnotations.PROVIDED_BY, 0)) {
      return;
    }
    if (AnnotationUtil.isAnnotated(boundClass, GuiceAnnotations.IMPLEMENTED_BY, 0)) {
      final PsiClass implementedByClass =
        AnnotationUtils.resolveAnnotationClassValue(boundClass, GuiceAnnotations.IMPLEMENTED_BY);
      if (referentClass.equals(implementedByClass)) {
        visitor.registerClassArgumentError(expression, referentClass);
      }
    }
    else if (boundClass.equals(referentClass)) {
      visitor.registerClassArgumentError(expression, referentClass);
    }
  }
}
