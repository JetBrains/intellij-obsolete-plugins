// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.model.beans.AssistedFactoryBindDescriptor;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;

import java.util.List;
import java.util.Set;

/**
 * Contributor for Guice {@code AssistedInject} bindings:
 * {@code FactoryModuleBuilder...build(Factory.class)}.
 */
public final class AssistedInjectContributor implements GuiceBindingContributor {

  private static final Set<String> BINDING_WORDS = Set.of("build");

  @Override
  public @NotNull Set<String> getBindingWords() {
    return BINDING_WORDS;
  }

  @Override
  public boolean processCall(@NotNull UCallExpression call,
                             @NotNull String methodName,
                             @NotNull String resolvedQName,
                             @NotNull PsiClass containingClass,
                             @NotNull Set<BindDescriptor> descriptors) {
    if (!"com.google.inject.assistedinject.FactoryModuleBuilder".equals(resolvedQName) &&
        !ContributorUtil.isGuicePackage(resolvedQName)) {
      return false;
    }

    final PsiElement sourcePsi = call.getSourcePsi();
    if (sourcePsi == null) return false;

    List<UExpression> args = call.getValueArguments();
    if (!args.isEmpty()) {
      PsiType factoryType = GuiceUtils.getBindingTypeFromExpression(args.getFirst());
      PsiClass factoryClass = factoryType instanceof PsiClassType ct ? ct.resolve() : null;
      descriptors.add(new AssistedFactoryBindDescriptor(sourcePsi, factoryClass));
      return true;
    }
    return false;
  }
}
