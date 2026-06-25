// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.beans;

import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SetMultibindDescriptor extends BindDescriptor {
  private final @Nullable SmartPsiElementPointer<PsiClass> myElementType;

  public SetMultibindDescriptor(@NotNull PsiElement callExpression, @Nullable PsiClass elementType) {
    super(callExpression);
    myElementType = elementType != null ? SmartPointerManager.createPointer(elementType) : null;
  }

  public @Nullable PsiClass getElementType() {
    return myElementType != null ? myElementType.getElement() : null;
  }

  @Override
  public @Nullable PsiClass calculateBindingClass() {
    return null;
  }

  @Override
  public boolean matchesType(@NotNull PsiType type) {
    final PsiType paramType = GuiceUtils.getTypeParameter(type, "java.util.Set", 0);
    final PsiClass paramClass = GuiceUtils.resolveClass(paramType);
    return GuiceUtils.areClassesEquivalent(paramClass, getElementType());
  }
}
