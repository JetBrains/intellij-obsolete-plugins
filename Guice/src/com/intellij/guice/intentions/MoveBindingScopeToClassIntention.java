// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.intentions;

import com.intellij.guice.GuiceBundle;
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
 * Replaces {@code bind(Foo.class).to(FooImpl.class).in(Scopes.SINGLETON)} with
 * {@code bind(Foo.class).to(FooImpl.class)} and {@code @Singleton} on the class that Guice constructs:
 * {@code FooImpl}, or {@code Foo} for an untargeted binding.
 */
public final class MoveBindingScopeToClassIntention extends BindingChainIntention {
  public MoveBindingScopeToClassIntention() {
    super(new MoveBindingScopeToClassPredicate(), GuiceBundle.message("move.binding.scope.to.class.text"));
  }

  @Override
  public @NotNull String getFamilyName() {
    return GuiceBundle.message("move.binding.scope.to.class.family.name");
  }

  @Override
  protected void invoke(@NotNull ActionContext context, @NotNull PsiMethodCallExpression element, @NotNull ModPsiUpdater updater) {
    UCallExpression uCall = UastContextKt.toUElement(element, UCallExpression.class);
    if (uCall == null) return;
    PsiClass scopedClass = GuiceUtils.findScopeAnnotatedClassForBinding(uCall);
    UCallExpression scopeCall = GuiceUtils.findCallInChain(uCall, "in");
    if (scopedClass == null || scopeCall == null || !(scopeCall.getSourcePsi() instanceof PsiMethodCallExpression psiScopeCall)) return;
    PsiExpression arg = psiScopeCall.getArgumentList().getExpressions()[0];
    String scopeAnnotation = GuiceUtils.getScopeAnnotationForScopeExpression(arg);
    if (scopeAnnotation == null) return;

    MutationUtils.addAnnotation(updater.getWritable(scopedClass), "@" + scopeAnnotation);
    PsiExpression qualifier = psiScopeCall.getMethodExpression().getQualifierExpression();
    if (qualifier != null) {
      MutationUtils.replaceExpression(qualifier.getText(), psiScopeCall);
    }
  }
}
