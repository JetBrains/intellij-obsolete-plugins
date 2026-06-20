// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.GuiceInjectionUtil;
import com.intellij.guice.model.InjectionPointDescriptor;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.MultimapBindDescriptor;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
  public boolean isRelevantType(@NotNull PsiType targetType) {
    return GuiceUtils.getMultimapValueType(targetType) != null;
  }

  @Override
  public @Nullable PsiType unwrapType(@NotNull PsiType type) {
    return GuiceUtils.getMultimapValueType(type);
  }

  @Override
  public void findMatchingBindings(@NotNull List<? extends BindDescriptor> descriptors,
                                   @NotNull PsiType targetType,
                                   @NotNull InjectionPointDescriptor ip,
                                   @NotNull Set<BindDescriptor> result) {
    for (BindDescriptor descriptor : descriptors) {
      if (!(descriptor instanceof MultimapBindDescriptor mmb)) continue;
      try {
        if (mmb.matchesType(targetType) && GuiceInjectionUtil.checkBindingAnnotations(ip, mmb)) {
          result.add(mmb);
        }
      }
      catch (PsiInvalidElementAccessException e) {
        // Stale PSI — skip.
      }
    }
  }

  @Override
  public @NotNull List<PsiElement> findMultibinderTargets(@NotNull PsiMethod providesMethod,
                                                          @NotNull List<? extends BindDescriptor> descriptors) {
    List<PsiElement> targets = new ArrayList<>();

    boolean isProvidesIntoMap =
        AnnotationUtil.isAnnotated(providesMethod, GuiceAnnotations.PROVIDES_INTO_MAP, 0) ||
        AnnotationUtil.isAnnotated(providesMethod, GuiceAnnotations.CHECKED_PROVIDES_INTO_MAP, 0);
    if (!isProvidesIntoMap) return targets;

    PsiType returnType = providesMethod.getReturnType();
    if (!(returnType instanceof PsiClassType returnClassType)) return targets;

    PsiClass returnClass = returnClassType.resolve();
    if (returnClass == null) return targets;

    for (BindDescriptor descriptor : descriptors) {
      if (!(descriptor instanceof MultimapBindDescriptor mmb)) continue;
      if (GuiceUtils.areClassesEquivalent(mmb.getValueType(), returnClass)) {
        PsiElement bindExpr = mmb.getBindExpression();
        if (bindExpr != null) targets.add(bindExpr);
      }
    }
    return targets;
  }
}
