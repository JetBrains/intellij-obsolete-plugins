// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.beans;

import com.intellij.guice.model.GuiceInjectionUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethodCallExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BindToConstructorDescriptor extends BindDescriptor {

  public BindToConstructorDescriptor(@NotNull PsiMethodCallExpression callExpression) {
    super(callExpression);
  }

  @Override
  public @Nullable PsiClass calculateBindingClass() {
    return GuiceInjectionUtil.getCallExpressionType(getBindExpression(), "toConstructor");
  }
}
