// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.beans;

import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MapMultibindDescriptor extends BindDescriptor {
  private final @Nullable PsiClass myKeyType;
  private final @Nullable PsiClass myValueType;

  public MapMultibindDescriptor(@NotNull PsiElement callExpression, @Nullable PsiClass keyType, @Nullable PsiClass valueType) {
    super(callExpression);
    myKeyType = keyType;
    myValueType = valueType;
  }

  public @Nullable PsiClass getKeyType() {
    return myKeyType;
  }

  public @Nullable PsiClass getValueType() {
    return myValueType;
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
    return GuiceUtils.areClassesEquivalent(keyClass, myKeyType) &&
           GuiceUtils.areClassesEquivalent(valClass, myValueType);
  }
}
