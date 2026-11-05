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
 * Replaces {@code bind(Foo.class).toProvider(FooProvider.class)} with {@code bind(Foo.class)} and
 * {@code @ProvidedBy(FooProvider.class)} on {@code Foo}.
 */
public final class MoveProviderBindingToClassIntention extends BindingChainIntention {
  public MoveProviderBindingToClassIntention() {
    super(new MoveProviderBindingToClassPredicate(), GuiceBundle.message("move.provider.binding.to.class.text"));
  }

  @Override
  public @NotNull String getFamilyName() {
    return GuiceBundle.message("move.provider.binding.to.class.family.name");
  }

  @Override
  protected void invoke(@NotNull ActionContext context, @NotNull PsiMethodCallExpression element, @NotNull ModPsiUpdater updater) {
    UCallExpression uCall = UastContextKt.toUElement(element, UCallExpression.class);
    if (uCall == null) return;
    PsiClass providerClass = GuiceInjectionUtil.getCallExpressionType(uCall, "toProvider");
    PsiClass boundClass = GuiceUtils.findImplementedClassForBinding(uCall);
    if (providerClass == null || boundClass == null) return;
    PsiClass writableBoundClass = updater.getWritable(boundClass);
    MutationUtils.addAnnotation(writableBoundClass, "@com.google.inject.ProvidedBy(" + providerClass.getQualifiedName() + ".class)");

    UCallExpression providerCall = GuiceUtils.findCallInChain(uCall, "toProvider");
    if (providerCall != null && providerCall.getSourcePsi() instanceof PsiMethodCallExpression psiProviderCall) {
      PsiExpression qualifier = psiProviderCall.getMethodExpression().getQualifierExpression();
      if (qualifier != null) {
        MutationUtils.replaceExpression(qualifier.getText(), psiProviderCall);
      }
    }
  }
}
