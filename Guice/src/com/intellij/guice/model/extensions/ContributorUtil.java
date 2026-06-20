// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.BindToConstructorDescriptor;
import com.intellij.guice.model.beans.BindToDescriptor;
import com.intellij.guice.model.beans.BindToInstanceDescriptor;
import com.intellij.guice.model.beans.BindToProviderDescriptor;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;

import java.util.List;
import java.util.Set;

/**
 * Shared utility methods for {@link GuiceBindingContributor} implementations.
 *
 * <p>These were originally private helpers in
 * {@code com.intellij.guice.model.GuiceInjectorManager} and are extracted here
 * so that multiple contributors can share them.
 */
final class ContributorUtil {

  private ContributorUtil() {
  }

  /**
   * Checks whether a fully qualified class name belongs to a Guice package.
   * Used as a relaxed fallback when exact class matching doesn't cover all
   * Guice internal builder classes.
   */
  static boolean isGuicePackage(@NotNull String qName) {
    return qName.startsWith("com.google.inject") || qName.startsWith("com.google.common.inject");
  }

  /**
   * Tries to create a binding-builder "tail" descriptor ({@code .to()}, {@code .toInstance()},
   * {@code .toProvider()}, {@code .toConstructor()}) from the given method name.
   *
   * @return {@code true} if a descriptor was created, {@code false} otherwise
   */
  static boolean createBindingTailDescriptor(@NotNull String methodName,
                                             @NotNull PsiElement outermostSource,
                                             @NotNull Set<BindDescriptor> descriptors) {
    switch (methodName) {
      case "to" -> descriptors.add(new BindToDescriptor(outermostSource));
      case "toInstance" -> descriptors.add(new BindToInstanceDescriptor(outermostSource));
      case "toProvider" -> descriptors.add(new BindToProviderDescriptor(outermostSource));
      case "toConstructor" -> descriptors.add(new BindToConstructorDescriptor(outermostSource));
      default -> { return false; }
    }
    return true;
  }

  /**
   * Returns the outermost qualified parent's source PSI element, or {@code null}.
   */
  static @Nullable PsiElement getOutermostSource(@NotNull UCallExpression call) {
    return GuiceUtils.getOutermostQualifiedParent(call).getSourcePsi();
  }

  /**
   * Returns the outermost call in a chained qualified expression, or the original call
   * if it is not part of a chain.
   */
  static @NotNull UCallExpression getOutermostCall(@NotNull UCallExpression bindCall) {
    UElement outermost = GuiceUtils.getOutermostQualifiedParent(bindCall);
    UExpression selector = GuiceUtils.getSelectorIfQualified(outermost);
    return selector instanceof UCallExpression ? (UCallExpression)selector : bindCall;
  }

  /**
   * Extracts a single type argument from a factory/binder call expression.
   * Tries explicit type arguments first, then falls back to the second value argument
   * (the first is typically the binder/module reference).
   *
   * @return the resolved {@link PsiClass}, or {@code null} if unavailable
   */
  static @Nullable PsiClass extractSingleTypeArg(@NotNull UCallExpression call) {
    PsiType type = null;
    List<PsiType> typeArgs = call.getTypeArguments();
    if (!typeArgs.isEmpty()) {
      type = typeArgs.getFirst();
    } else {
      List<UExpression> args = call.getValueArguments();
      if (args.size() > 1) {
        type = GuiceUtils.getBindingTypeFromExpression(args.get(1));
      }
    }
    return type instanceof PsiClassType ct ? ct.resolve() : null;
  }

  /**
   * Extracts a key–value type argument pair from a MapBinder/MultimapBinder call expression.
   * Tries explicit type arguments first, then falls back to value arguments at indices 1 and 2.
   *
   * @return a two-element array {@code [keyClass, valClass]}; either element may be {@code null}
   */
  static PsiClass @NotNull [] extractDualTypeArgs(@NotNull UCallExpression call) {
    PsiType keyType = null;
    PsiType valType = null;
    List<PsiType> typeArgs = call.getTypeArguments();
    if (typeArgs.size() > 1) {
      keyType = typeArgs.get(0);
      valType = typeArgs.get(1);
    } else {
      List<UExpression> args = call.getValueArguments();
      if (args.size() > 2) {
        keyType = GuiceUtils.getBindingTypeFromExpression(args.get(1));
        valType = GuiceUtils.getBindingTypeFromExpression(args.get(2));
      }
    }
    PsiClass keyClass = keyType instanceof PsiClassType kct ? kct.resolve() : null;
    PsiClass valClass = valType instanceof PsiClassType vct ? vct.resolve() : null;
    return new PsiClass[]{keyClass, valClass};
  }
}
