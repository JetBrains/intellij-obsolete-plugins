// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.beans;

import com.intellij.guice.model.GuiceInjectionUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;

public class BindToDescriptor extends BindDescriptor {

  public BindToDescriptor(@NotNull PsiElement callExpression) {
    super(callExpression);
  }

  @Override
  public @Nullable PsiClass calculateBindingClass() {
    final UCallExpression uCall = getOutermostCall();
    return uCall != null ? GuiceInjectionUtil.getCallExpressionType(uCall, "to") : null;
  }
}
