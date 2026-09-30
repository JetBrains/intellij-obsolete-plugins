// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.constants.GuiceClasses;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.UntargetedBindDescriptor;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UQualifiedReferenceExpression;

import java.util.Set;

/**
 * Contributor for standard Guice bindings: {@code bind().to()}, {@code bind().toInstance()},
 * {@code bind().toProvider()}, {@code bind().toConstructor()}, and untargeted {@code bind()}.
 */
final class StandardBindingContributor implements GuiceBindingContributor {

  private static final Set<String> BINDING_WORDS = Set.of(
    "to", "toInstance", "toProvider", "toConstructor", "bind"
  );

  private static final Set<String> BINDING_CHAIN_HEADS = Set.of(
    "bind", "bindConstant", "addBinding", "setDefault", "setBinding"
  );

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
    final PsiElement sourcePsi = call.getSourcePsi();
    if (sourcePsi == null) return false;

    boolean handled = false;

    // Binding-builder tail methods: .to(), .toInstance(), .toProvider(), .toConstructor()
    if (ContributorUtil.isBindingBuilderMethod(resolvedQName, call, containingClass,
                                                GuiceClasses.LINKED_BINDING_BUILDER) ||
        ContributorUtil.isBindingBuilderMethod(resolvedQName, call, containingClass,
                                                GuiceClasses.CONSTANT_BINDING_BUILDER)) {

      PsiElement outermostSource = ContributorUtil.getOutermostSource(call);
      if (outermostSource != null) {
        handled = ContributorUtil.createBindingTailDescriptor(methodName, outermostSource, descriptors);
      }
    }

    // Untargeted bind()
    if ("bind".equals(methodName) &&
        ("com.google.inject.Binder".equals(resolvedQName) ||
         "com.google.inject.AbstractModule".equals(resolvedQName) ||
         "com.google.inject.PrivateModule".equals(resolvedQName) ||
         InheritanceUtil.isInheritor(containingClass, "com.google.inject.Binder") ||
         InheritanceUtil.isInheritor(containingClass, "com.google.inject.AbstractModule"))) {

      UCallExpression outermostCall = ContributorUtil.getOutermostCall(call);
      if (GuiceUtils.isUntargetedBinding(outermostCall)) {
        PsiElement outermostSource = ContributorUtil.getOutermostSource(call);
        if (outermostSource != null) {
          descriptors.add(new UntargetedBindDescriptor(outermostSource));
          handled = true;
        }
      }
    }

    return handled;
  }

  @Override
  public boolean processUnresolvedCall(@NotNull UCallExpression call,
                                       @NotNull String methodName,
                                       @NotNull Set<BindDescriptor> descriptors) {
    UCallExpression outermostCall = ContributorUtil.getOutermostCall(call);
    if (!hasBindingChainHead(outermostCall)) {
      return false;
    }
    PsiElement outermostSource = outermostCall.getSourcePsi();
    if (outermostSource != null) {
      return ContributorUtil.createBindingTailDescriptor(methodName, outermostSource, descriptors);
    }
    return false;
  }

  private static boolean hasBindingChainHead(@NotNull UCallExpression call) {
    UExpression current = call;
    while (current != null) {
      if (current instanceof UCallExpression callExpr) {
        String name = callExpr.getMethodName();
        if (name != null && BINDING_CHAIN_HEADS.contains(name)) {
          return true;
        }
        current = callExpr.getReceiver();
      }
      else if (current instanceof UQualifiedReferenceExpression qualifiedRef) {
        UExpression selector = qualifiedRef.getSelector();
        if (selector instanceof UCallExpression selectorCall) {
          String name = selectorCall.getMethodName();
          if (name != null && BINDING_CHAIN_HEADS.contains(name)) {
            return true;
          }
        }
        current = qualifiedRef.getReceiver();
      }
      else {
        break;
      }
    }
    return false;
  }
}
