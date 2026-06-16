// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.beans;

import com.intellij.guice.model.GuiceInjectionUtil;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethodCallExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UastContextKt;

public class BindToInstanceDescriptor extends BindDescriptor {

  public BindToInstanceDescriptor(@NotNull com.intellij.psi.PsiElement callExpression) {
    super(callExpression);
  }

  @Override
  public @Nullable PsiClass calculateBindingClass() {
    final UCallExpression uCall = GuiceUtils.getCallExpression(getBindExpression());
    return uCall != null ? GuiceInjectionUtil.getCallExpressionType(uCall, "toInstance") : null;
  }
}
