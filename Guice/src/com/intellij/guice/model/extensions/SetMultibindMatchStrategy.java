// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.GuiceInjectionUtil;
import com.intellij.guice.model.jam.GuiceProvides;
import com.intellij.guice.model.InjectionPointDescriptor;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.SetMultibindDescriptor;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.*;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Matches {@link SetMultibindDescriptor}s against injection points of type
 * {@code Set<T>} and finds multibinder targets for {@code @ProvidesIntoSet} methods.
 */
public final class SetMultibindMatchStrategy implements GuiceBindingMatchStrategy {

  @Override
  public @NotNull Class<? extends BindDescriptor> getDescriptorClass() {
    return SetMultibindDescriptor.class;
  }

  @Override
  public boolean isRelevantType(@NotNull PsiType targetType) {
    return GuiceUtils.getMultibinderElementType(targetType) != null;
  }

  @Override
  public @Nullable PsiType unwrapType(@NotNull PsiType type) {
    return GuiceUtils.getMultibinderElementType(type);
  }

  @Override
  public void findMatchingBindings(@NotNull List<? extends BindDescriptor> descriptors,
                                   @NotNull PsiType targetType,
                                   @NotNull InjectionPointDescriptor ip,
                                   @NotNull Set<BindDescriptor> result) {
    for (BindDescriptor descriptor : descriptors) {
      if (!(descriptor instanceof SetMultibindDescriptor smb)) continue;
      try {
        if (smb.matchesType(targetType) && GuiceInjectionUtil.checkBindingAnnotations(ip, smb)) {
          result.add(smb);
        }
      }
      catch (PsiInvalidElementAccessException e) {
        // Stale PSI — skip.
      }
    }
  }

  @Override
  public void findMatchingProvides(@NotNull Set<GuiceProvides> candidates,
                                   @NotNull PsiType unwrappedType,
                                   @NotNull Set<PsiAnnotation> ipAnnotations,
                                   @NotNull Set<GuiceProvides> result) {
    for (GuiceProvides provides : candidates) {
      try {
        PsiType productType = provides.getProductType();
        if (productType != null &&
            TypeConversionUtil.isAssignable(unwrappedType, productType) &&
            (AnnotationUtil.isAnnotated(provides.getPsiElement(),
                GuiceAnnotations.PROVIDES_INTO_SET, 0) ||
             AnnotationUtil.isAnnotated(provides.getPsiElement(),
                GuiceAnnotations.CHECKED_PROVIDES_INTO_SET, 0)) &&
            GuiceInjectionUtil.checkBindingAnnotations(
                ipAnnotations, provides.getBindingAnnotations())) {
          result.add(provides);
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

    boolean isProvidesIntoSet =
        AnnotationUtil.isAnnotated(providesMethod, GuiceAnnotations.PROVIDES_INTO_SET, 0) ||
        AnnotationUtil.isAnnotated(providesMethod, GuiceAnnotations.CHECKED_PROVIDES_INTO_SET, 0);
    if (!isProvidesIntoSet) return targets;

    PsiType returnType = providesMethod.getReturnType();
    if (!(returnType instanceof PsiClassType returnClassType)) return targets;

    PsiClass returnClass = returnClassType.resolve();
    if (returnClass == null) return targets;

    for (BindDescriptor descriptor : descriptors) {
      if (!(descriptor instanceof SetMultibindDescriptor smb)) continue;
      if (GuiceUtils.areClassesEquivalent(smb.getElementType(), returnClass)) {
        PsiElement bindExpr = smb.getBindExpression();
        if (bindExpr != null) targets.add(bindExpr);
      }
    }
    return targets;
  }
}
