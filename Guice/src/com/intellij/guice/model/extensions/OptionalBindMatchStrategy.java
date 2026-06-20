// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.model.GuiceInjectionUtil;
import com.intellij.guice.model.InjectionPointDescriptor;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.OptionalBindDescriptor;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Matches {@link OptionalBindDescriptor}s against injection points of type
 * {@code Optional<T>} (both {@code java.util.Optional} and
 * {@code com.google.common.base.Optional}).
 */
public final class OptionalBindMatchStrategy implements GuiceBindingMatchStrategy {

  @Override
  public @NotNull Class<? extends BindDescriptor> getDescriptorClass() {
    return OptionalBindDescriptor.class;
  }

  @Override
  public boolean isRelevantType(@NotNull PsiType targetType) {
    return GuiceUtils.getOptionalType(targetType) != null;
  }

  @Override
  public @Nullable PsiType unwrapType(@NotNull PsiType type) {
    return GuiceUtils.getOptionalType(type);
  }

  @Override
  public void findMatchingBindings(@NotNull List<? extends BindDescriptor> descriptors,
                                   @NotNull PsiType targetType,
                                   @NotNull InjectionPointDescriptor ip,
                                   @NotNull Set<BindDescriptor> result) {
    for (BindDescriptor descriptor : descriptors) {
      if (!(descriptor instanceof OptionalBindDescriptor opt)) continue;
      try {
        if (opt.matchesType(targetType) && GuiceInjectionUtil.checkBindingAnnotations(ip, opt)) {
          result.add(opt);
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
    PsiType returnType = providesMethod.getReturnType();
    if (!(returnType instanceof PsiClassType returnClassType)) return targets;

    PsiClass returnClass = returnClassType.resolve();
    if (returnClass == null) return targets;

    for (BindDescriptor descriptor : descriptors) {
      if (!(descriptor instanceof OptionalBindDescriptor opt)) continue;
      if (GuiceUtils.areClassesEquivalent(opt.getOptionalBoundClass(), returnClass)) {
        PsiElement bindExpr = opt.getBindExpression();
        if (bindExpr != null) targets.add(bindExpr);
      }
    }
    return targets;
  }
}
