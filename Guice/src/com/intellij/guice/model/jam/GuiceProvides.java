// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.jam;

import com.intellij.guice.model.GuiceInjectorManager;
import com.intellij.guice.model.InjectionPointDescriptor;
import com.intellij.jam.JamBaseElement;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.psi.*;
import com.intellij.semantic.SemKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class GuiceProvides<T extends PsiMember> extends JamBaseElement<T> {
  public static final SemKey<GuiceProvides> SEM_KEY = SemKey.createKey("GuiceProvides");
  public static final JamMethodMeta<GuiceProvides> METHOD_META = new JamMethodMeta<>(null, ref -> new Method(ref), SEM_KEY);

  protected GuiceProvides(PsiElementRef<?> ref) {
    super(ref);
  }

  public abstract List<InjectionPointDescriptor> getInjectionPoints();

  public @NotNull Set<PsiAnnotation> getBindingAnnotations() {
    return GuiceInjectorManager.getBindingAnnotations(getPsiElement());
  }

  public abstract @Nullable PsiType getProductType();

  public static final class Method extends GuiceProvides<PsiMethod> {
    public Method(PsiElementRef<?> ref) {
      super(ref);
    }

    @Override
    public PsiType getProductType() {
      return getPsiElement().getReturnType();
    }

    @Override
    public List<InjectionPointDescriptor> getInjectionPoints() {
      List<InjectionPointDescriptor> descriptors = new ArrayList<>();
      for (PsiParameter parameter : getPsiElement().getParameterList().getParameters()) {
        descriptors.add(new InjectionPointDescriptor(parameter));
      }
      return descriptors;
    }
  }
}
