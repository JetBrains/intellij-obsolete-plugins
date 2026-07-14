// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.MultimapBindDescriptor;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Matches {@link MultimapBindDescriptor}s against injection points of type
 * {@code Multimap<K, V>} and finds multibinder targets for {@code @ProvidesIntoMap} methods.
 */
public final class MultimapBindMatchStrategy implements GuiceBindingMatchStrategy {

  @Override
  public @NotNull Class<? extends BindDescriptor> getDescriptorClass() {
    return MultimapBindDescriptor.class;
  }

  @Override
  public @NotNull Collection<String> getProvidesAnnotations() {
    return List.of(GuiceAnnotations.PROVIDES_INTO_MAP, GuiceAnnotations.CHECKED_PROVIDES_INTO_MAP);
  }

  @Override
  public @Nullable PsiType unwrapType(@NotNull PsiType type) {
    return GuiceUtils.getMultimapValueType(type);
  }

  @Override
  public @Nullable PsiType wrapType(@NotNull BindDescriptor descriptor) {
    if (!(descriptor instanceof MultimapBindDescriptor mbd)) return null;
    PsiClass keyClass = mbd.getKeyType();
    PsiClass valueClass = mbd.getValueType();
    PsiElement bindExpr = descriptor.getBindExpression();
    if (keyClass == null || valueClass == null || bindExpr == null) return null;
    PsiClass multimapClass = JavaPsiFacade.getInstance(bindExpr.getProject())
        .findClass("com.google.common.collect.Multimap", bindExpr.getResolveScope());
    if (multimapClass == null) return null;
    PsiElementFactory factory = JavaPsiFacade.getElementFactory(bindExpr.getProject());
    return factory.createType(multimapClass, factory.createType(keyClass), factory.createType(valueClass));
  }

  @Override
  public @NotNull List<PsiElement> findMultibinderTargets(@NotNull PsiMethod providesMethod,
                                                          @NotNull List<? extends BindDescriptor> descriptors) {
    List<PsiElement> targets = new ArrayList<>();

    if (!isProvidesIntoMethod(providesMethod)) return targets;

    PsiType returnType = providesMethod.getReturnType();
    if (!(returnType instanceof PsiClassType returnClassType)) return targets;

    PsiClass returnClass = returnClassType.resolve();
    if (returnClass == null) return targets;

    // Resolve the provides method's @MapKey annotation to determine its key type.
    PsiClass providesKeyClass = MapMultibindMatchStrategy.resolveMapKeyType(providesMethod);

    for (BindDescriptor descriptor : descriptors) {
      if (!(descriptor instanceof MultimapBindDescriptor mmb)) continue;
      if (GuiceUtils.areClassesEquivalent(mmb.getValueType(), returnClass) &&
          (providesKeyClass == null || GuiceUtils.areClassesEquivalent(mmb.getKeyType(), providesKeyClass))) {
        PsiElement bindExpr = mmb.getBindExpression();
        if (bindExpr != null) targets.add(bindExpr);
      }
    }
    return targets;
  }
}
