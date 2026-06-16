// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.beans;

import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Binding descriptor for {@code MultimapBinder.newSetMultimapBinder(binder, K.class, V.class)}.
 *
 * <p>Matches injection points of type {@code Multimap<K, V>}
 * (i.e. {@code com.google.common.collect.Multimap}).
 */
public class MultimapBindDescriptor extends BindDescriptor {
  private final @Nullable PsiClass myKeyType;
  private final @Nullable PsiClass myValueType;

  public MultimapBindDescriptor(@NotNull PsiElement callExpression, @Nullable PsiClass keyType, @Nullable PsiClass valueType) {
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
    final PsiType keyParam = GuiceUtils.getTypeParameter(type, "com.google.common.collect.Multimap", 0);
    final PsiType valParam = GuiceUtils.getTypeParameter(type, "com.google.common.collect.Multimap", 1);
    final PsiClass keyClass = GuiceUtils.resolveClass(keyParam);
    final PsiClass valClass = GuiceUtils.resolveClass(valParam);
    return GuiceUtils.areClassesEquivalent(keyClass, myKeyType) &&
           GuiceUtils.areClassesEquivalent(valClass, myValueType);
  }
}
