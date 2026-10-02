// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.model.beans.AssistedFactoryBindDescriptor;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;

/**
 * Contributor for Guice {@code AssistedInject} bindings:
 * {@code FactoryModuleBuilder...build(Factory.class)}.
 */
final class AssistedInjectContributor implements GuiceBindingContributor {
  @Override
  public void register(@NotNull GuiceExtensionRegistrar registrar) {
    registrar.registerCallDescriptor(
        GuiceCallPattern.named("build")
            .withOwnerClass("com.google.inject.assistedinject.FactoryModuleBuilder")
            .minArguments(1),
        (call, descriptors) -> {
          PsiElement sourcePsi = call.getSourcePsi();
          if (sourcePsi == null) return false;
          PsiType factoryType = GuiceUtils.getBindingTypeFromExpression(call.getValueArguments().getFirst());
          descriptors.add(new AssistedFactoryBindDescriptor(sourcePsi, factoryType));
          return true;
        }
    );
  }
}
