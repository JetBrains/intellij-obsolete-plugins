// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceClasses;
import com.intellij.guice.model.extensions.GuiceCallPattern;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UastUtils;

/**
 * Reports {@code .to()} bindings that target a class Guice cannot instantiate
 * (e.g., an abstract class or interface with no {@code @Inject} constructor),
 * unless a {@code @Provides} method supplies it.
 */
public final class UninstantiableBindingInspection extends BaseUastInspection {
  public UninstantiableBindingInspection() {
    extendCall(
        GuiceCallPattern.named("to")
            .requireValueOrTypeArgument()
            .withOwnerClass(GuiceClasses.LINKED_BINDING_BUILDER)
            .withReceiverInheritor(GuiceClasses.LINKED_BINDING_BUILDER),
        UninstantiableBindingInspection::checkToCall
    );
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("uninstantiable.binding.problem.descriptor");
  }

  private static void checkToCall(@NotNull UCallExpression expression, @NotNull BaseUastInspectionVisitor visitor) {
    PsiClass referentClass = GuiceUtils.resolveClassArgument(expression);
    if (referentClass == null || GuiceUtils.isInstantiable(referentClass)) {
      return;
    }
    UClass moduleUClass = UastUtils.getParentOfType(expression, UClass.class);
    PsiClass moduleClass = moduleUClass != null ? moduleUClass.getJavaPsi() : null;
    if (moduleClass != null && GuiceUtils.provides(moduleClass, referentClass)) {
      return;
    }
    visitor.registerClassArgumentError(expression);
  }
}
