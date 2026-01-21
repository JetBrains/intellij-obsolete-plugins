// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.beans;

import com.intellij.guice.model.GuiceInjectionUtil;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethodCallExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BindToProviderDescriptor extends BindDescriptor {
  private final NullableLazyValue<PsiClass> myProviderClass = new NullableLazyValue<>() {
    @Override
    protected @Nullable PsiClass compute() {
      return GuiceInjectionUtil.getCallExpressionType(getBindExpression(), "toProvider");
    }
  };

  public BindToProviderDescriptor(@NotNull PsiMethodCallExpression callExpression) {
    super(callExpression);
  }

  @Override
  public @Nullable PsiClass calculateBindingClass() {
    final PsiClass providerClass = getProviderClass();
    if (providerClass != null) {
      return GuiceUtils.getProvidedType(providerClass);
    }
    return null;
  }

  public @Nullable PsiClass getProviderClass() {
    return myProviderClass.getValue();
  }
}
