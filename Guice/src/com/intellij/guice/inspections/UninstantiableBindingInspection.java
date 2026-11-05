// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceClasses;
import com.intellij.guice.model.GuiceInjectorManager;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.UntargetedBindDescriptor;
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
    GuiceCallPattern toCall = GuiceCallPattern.named("to").requireValueOrTypeArgument();
    extendCall(
        // to() of LinkedBindingBuilder itself, or an extension function (Kotlin) on such a receiver
        GuiceCallPattern.anyOf(
            toCall.withOwnerClass(GuiceClasses.LINKED_BINDING_BUILDER),
            toCall.withReceiverInheritor(GuiceClasses.LINKED_BINDING_BUILDER)),
        UninstantiableBindingInspection::checkToCall
    );
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("uninstantiable.binding.problem.descriptor", infos);
  }

  private static void checkToCall(@NotNull UCallExpression expression, @NotNull BaseUastInspectionVisitor visitor) {
    // to(Key.get(T, qualifier)) links to another binding of T; Guice constructs nothing here.
    if (GuiceUtils.linksToQualifiedKey(expression)) {
      return;
    }
    PsiClass referentClass = GuiceUtils.resolveClassArgument(expression);
    if (referentClass == null || GuiceUtils.isInstantiable(referentClass)) {
      return;
    }
    // Guice resolves a target with @ImplementedBy or @ProvidedBy without a constructor.
    if (GuiceUtils.hasJitBindingAnnotation(referentClass)) {
      return;
    }
    UClass moduleUClass = UastUtils.getParentOfType(expression, UClass.class);
    PsiClass moduleClass = moduleUClass != null ? moduleUClass.getJavaPsi() : null;
    if (moduleClass != null && (GuiceUtils.provides(moduleClass, referentClass) || isBoundToTarget(moduleClass, referentClass))) {
      return;
    }
    visitor.registerClassArgumentError(expression, referentClass);
  }

  /**
   * Tells if the module binds the class itself to a target, for example {@code bind(X.class).toProvider(...)},
   * so a link to {@code X} needs no constructor of {@code X}.
   */
  private static boolean isBoundToTarget(@NotNull PsiClass moduleClass, @NotNull PsiClass boundClass) {
    for (BindDescriptor descriptor : GuiceInjectorManager.getBindingDescriptors(moduleClass)) {
      if (descriptor instanceof UntargetedBindDescriptor || descriptor.isSpecialBinder()) continue;
      UCallExpression chain = descriptor.getOutermostCall();
      if (chain == null || !GuiceUtils.isStandardBindChain(chain) || GuiceUtils.isQualifiedBindChain(chain)) continue;
      if (GuiceUtils.areClassesEquivalent(descriptor.getBoundClass(), boundClass)) return true;
    }
    return false;
  }
}
