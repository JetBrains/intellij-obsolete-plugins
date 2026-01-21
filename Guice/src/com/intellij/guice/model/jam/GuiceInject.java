// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.jam;

import com.intellij.guice.model.GuiceInjectorManager;
import com.intellij.guice.model.InjectionPointDescriptor;
import com.intellij.jam.JamBaseElement;
import com.intellij.jam.reflect.JamFieldMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.psi.*;
import com.intellij.semantic.SemKey;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class GuiceInject<T extends PsiMember> extends JamBaseElement<T> {
  public static final SemKey<GuiceInject> SEM_KEY = SemKey.createKey("GuiceInject");
  public static final JamMethodMeta<GuiceInject> METHOD_JSR_330_META = new JamMethodMeta<>(null, ref -> new Method(ref), SEM_KEY);
  public static final JamMethodMeta<GuiceInject> METHOD_JAKARTA_META = new JamMethodMeta<>(null, ref -> new Method(ref), SEM_KEY);
  public static final JamMethodMeta<GuiceInject> METHOD_META = new JamMethodMeta<>(null, ref -> new Method(ref), SEM_KEY);
  public static final JamFieldMeta<GuiceInject> FIELD_JSR_330_META = new JamFieldMeta<>(null, ref -> new Field(ref), SEM_KEY);
  public static final JamFieldMeta<GuiceInject> FIELD_JAKARTA_META = new JamFieldMeta<>(null, ref -> new Field(ref), SEM_KEY);
  public static final JamFieldMeta<GuiceInject> FIELD_META = new JamFieldMeta<>(null, ref -> new Field(ref), SEM_KEY);

  protected GuiceInject(PsiElementRef<?> ref) {
    super(ref);
  }

  public abstract List<InjectionPointDescriptor> getInjectionPoints();

  public @NotNull Set<PsiAnnotation> getBindingAnnotations() {
    return GuiceInjectorManager.getBindingAnnotations(getPsiElement());
  }

  public static class Field extends GuiceInject<PsiField> {
    public Field(PsiElementRef<?> ref) {
      super(ref);
    }

    public PsiType getType() {
      return getPsiElement().getType();
    }

    @Override
    public List<InjectionPointDescriptor> getInjectionPoints() {
      return Collections.singletonList(new InjectionPointDescriptor(getPsiElement()));
    }
  }

  public static class Method extends GuiceInject<PsiMethod> {
    public Method(PsiElementRef<?> ref) {
      super(ref);
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
