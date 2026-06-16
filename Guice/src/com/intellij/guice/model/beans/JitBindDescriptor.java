// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.beans;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JitBindDescriptor extends BindDescriptor {
  private final PsiClass myClass;

  public JitBindDescriptor(@NotNull PsiMethod constructor, @NotNull PsiClass aClass) {
    super(constructor);
    myClass = aClass;
  }

  @Override
  public @Nullable PsiClass getBoundClass() {
    return myClass;
  }

  @Override
  public @Nullable PsiClass calculateBindingClass() {
    return myClass;
  }
}
