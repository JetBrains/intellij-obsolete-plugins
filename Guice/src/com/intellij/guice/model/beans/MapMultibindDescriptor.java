// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.beans;

import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MapMultibindDescriptor extends BindDescriptor {
  private final @Nullable SmartPsiElementPointer<PsiClass> myKeyType;
  private final @Nullable SmartPsiElementPointer<PsiClass> myValueType;

  public MapMultibindDescriptor(@NotNull PsiElement callExpression, @Nullable PsiClass keyType, @Nullable PsiClass valueType) {
    super(callExpression);
    myKeyType = keyType != null ? SmartPointerManager.createPointer(keyType) : null;
    myValueType = valueType != null ? SmartPointerManager.createPointer(valueType) : null;
  }

  public @Nullable PsiClass getKeyType() {
    return myKeyType != null ? myKeyType.getElement() : null;
  }

  public @Nullable PsiClass getValueType() {
    return myValueType != null ? myValueType.getElement() : null;
  }

  @Override
  public @Nullable PsiClass calculateBindingClass() {
    return null;
  }

  @Override
  public boolean matchesType(@NotNull PsiType type) {
    final PsiType keyParam = GuiceUtils.getTypeParameter(type, "java.util.Map", 0);
    final PsiType valParam = GuiceUtils.getTypeParameter(type, "java.util.Map", 1);
    final PsiClass keyClass = GuiceUtils.resolveClass(keyParam);
    final PsiClass valClass = GuiceUtils.resolveClass(valParam);
    return GuiceUtils.areClassesEquivalent(keyClass, getKeyType()) &&
           GuiceUtils.areClassesEquivalent(valClass, getValueType());
  }
}
