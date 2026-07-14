// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.beans;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JitBindDescriptor extends BindDescriptor {
  private final SmartPsiElementPointer<PsiClass> myClass;

  public JitBindDescriptor(@NotNull PsiMethod constructor, @NotNull PsiClass aClass) {
    super(constructor);
    myClass = SmartPointerManager.createPointer(aClass);
  }

  @Override
  public @Nullable PsiClass getBoundClass() {
    return myClass.getElement();
  }

  @Override
  public @Nullable PsiClass calculateBindingClass() {
    return myClass.getElement();
  }
}
