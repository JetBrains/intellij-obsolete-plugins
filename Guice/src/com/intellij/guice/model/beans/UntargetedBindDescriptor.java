// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.beans;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UntargetedBindDescriptor extends BindDescriptor {

  public UntargetedBindDescriptor(@NotNull PsiElement callExpression) {
    super(callExpression);
  }

  @Override
  public @Nullable PsiClass calculateBindingClass() {
    return getBoundClass();
  }
}
