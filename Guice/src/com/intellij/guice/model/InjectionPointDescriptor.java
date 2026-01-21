// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;


public class InjectionPointDescriptor {
  private final PsiModifierListOwner myOwner;

  public InjectionPointDescriptor(PsiModifierListOwner owner) {
    myOwner = owner;
  }

  public @Nullable PsiType getType() {
    if (myOwner instanceof PsiField) return ((PsiField)myOwner).getType();
    if (myOwner instanceof PsiParameter) return ((PsiParameter)myOwner).getType();

    return null;
  }

  public Set<PsiAnnotation> getBindingAnnotations() {
    return GuiceInjectorManager.getBindingAnnotations(myOwner);
  }

  public @NotNull PsiModifierListOwner getOwner() {
    return myOwner;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof InjectionPointDescriptor that)) return false;

    if (myOwner != null ? !myOwner.equals(that.myOwner) : that.myOwner != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myOwner != null ? myOwner.hashCode() : 0;
  }
}
