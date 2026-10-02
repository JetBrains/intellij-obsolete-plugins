// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.constants.GuiceClasses;
import com.intellij.guice.model.GuiceEntryProducer;
import com.intellij.guice.model.beans.UntargetedBindDescriptor;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UCallExpression;

/**
 * Contributor for standard Guice bindings: {@code bind().to()}, {@code bind().toInstance()},
 * {@code bind().toProvider()}, {@code bind().toConstructor()}, untargeted {@code bind()},
 * and module {@code getProvider()} calls.
 */
final class StandardBindingContributor implements GuiceBindingContributor {
  @Override
  public void register(@NotNull GuiceExtensionRegistrar registrar) {
    registrar.registerCallDescriptor(
        GuiceCallPattern.named("to", "toInstance", "toProvider", "toConstructor")
            .forBindingBuilder(GuiceClasses.LINKED_BINDING_BUILDER, GuiceClasses.CONSTANT_BINDING_BUILDER)
            .allowUnresolved("bind", "bindConstant", "addBinding", "setDefault", "setBinding"),
        (call, descriptors) -> {
          String methodName = call.getMethodName();
          PsiElement outermostSource = ContributorUtil.getOutermostSource(call);
          return methodName != null
              && outermostSource != null
              && ContributorUtil.createBindingTailDescriptor(methodName, outermostSource, descriptors);
        }
    );

    registrar.registerCallDescriptor(
        GuiceCallPattern.named("bind")
            .withOwnerInheritor(
                "com.google.inject.Binder",
                "com.google.inject.AbstractModule",
                "com.google.inject.PrivateModule"
            ),
        (call, descriptors) -> {
          UCallExpression outermostCall = ContributorUtil.getOutermostCall(call);
          if (GuiceUtils.isUntargetedBinding(outermostCall)) {
            PsiElement outermostSource = ContributorUtil.getOutermostSource(call);
            if (outermostSource != null) {
              descriptors.add(new UntargetedBindDescriptor(outermostSource));
              return true;
            }
          }
          return false;
        }
    );

    registrar.registerModuleCallEntry(
        GuiceCallPattern.named("getProvider")
            .withOwnerClass(
                "com.google.inject.Binder",
                "com.google.inject.PrivateBinder",
                "com.google.inject.AbstractModule",
                "com.google.inject.PrivateModule"
            )
            .argumentCount(1),
        GuiceEntryProducer::addGetProviderEntry
    );
  }
}
