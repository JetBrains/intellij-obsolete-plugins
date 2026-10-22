// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.intentions;

import com.intellij.guice.GuiceBundle;
import com.intellij.guice.model.GuiceInjectionUtil;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.guice.utils.MutationUtils;
import com.intellij.modcommand.ActionContext;
import com.intellij.modcommand.ModPsiUpdater;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UastContextKt;

/**
 * Replaces {@code bind(Foo.class).to(FooImpl.class)} with {@code bind(Foo.class)} and
 * {@code @ImplementedBy(FooImpl.class)} on {@code Foo}.
 */
public final class MoveBindingToClassIntention extends BindingChainIntention {
  public MoveBindingToClassIntention() {
    super(new MoveBindingToClassPredicate(), GuiceBundle.message("move.binding.to.class.text"));
  }

  @Override
  public @NotNull String getFamilyName() {
    return GuiceBundle.message("move.binding.to.class.family.name");
  }

  @Override
  protected void invoke(@NotNull ActionContext context, @NotNull PsiMethodCallExpression element, @NotNull ModPsiUpdater updater) {
    UCallExpression uCall = UastContextKt.toUElement(element, UCallExpression.class);
    if (uCall == null) return;
    PsiClass implementingClass = GuiceInjectionUtil.getCallExpressionType(uCall, "to");
    PsiClass boundClass = GuiceUtils.findImplementedClassForBinding(uCall);
    if (implementingClass == null || boundClass == null) return;
    PsiClass writableBoundClass = updater.getWritable(boundClass);
    MutationUtils.addAnnotation(writableBoundClass, "@com.google.inject.ImplementedBy(" + implementingClass.getQualifiedName() + ".class)");

    UCallExpression toCall = GuiceUtils.findCallInChain(uCall, "to");
    if (toCall != null && toCall.getSourcePsi() instanceof PsiMethodCallExpression psiToCall) {
      PsiExpression qualifier = psiToCall.getMethodExpression().getQualifierExpression();
      if (qualifier != null) {
        MutationUtils.replaceExpression(qualifier.getText(), psiToCall);
      }
    }
  }
}
