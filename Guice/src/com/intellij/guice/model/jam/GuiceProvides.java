// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.jam;

import com.intellij.guice.model.GuiceInjectorManager;
import com.intellij.guice.model.InjectionPointDescriptor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class GuiceProvides {
  private final SmartPsiElementPointer<PsiMethod> myMethodPointer;

  public GuiceProvides(@NotNull PsiMethod method) {
    myMethodPointer = SmartPointerManager.createPointer(method);
  }

  /**
   * Returns the {@code @Provides} method, or {@code null} if the element has been deleted.
   */
  public @Nullable PsiMethod getPsiElement() {
    return myMethodPointer.getElement();
  }

  public @Nullable PsiType getProductType() {
    PsiMethod method = myMethodPointer.getElement();
    if (method == null) return null;
    return method.getReturnType();
  }

  public @NotNull Set<PsiAnnotation> getBindingAnnotations() {
    PsiMethod method = myMethodPointer.getElement();
    if (method == null) return Set.of();
    return GuiceInjectorManager.getBindingAnnotations(method);
  }

  public List<InjectionPointDescriptor> getInjectionPoints() {
    PsiMethod method = myMethodPointer.getElement();
    if (method == null) return List.of();
    List<InjectionPointDescriptor> descriptors = new ArrayList<>();
    for (PsiParameter parameter : method.getParameterList().getParameters()) {
      descriptors.add(new InjectionPointDescriptor(parameter));
    }
    return descriptors;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GuiceProvides provides = (GuiceProvides) o;
    return myMethodPointer.equals(provides.myMethodPointer);
  }

  @Override
  public int hashCode() {
    return myMethodPointer.hashCode();
  }
}
