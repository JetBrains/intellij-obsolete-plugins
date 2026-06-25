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
import com.intellij.psi.util.InheritanceUtil;
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
   * Checks whether the resolved method belongs to or returns the given binder class,
   * using UAST for language-agnostic type resolution.
   *
   * <p>This handles two cases:
   * <ol>
   *   <li><b>Java / direct call</b>: {@code MapBinder.newMapBinder(...)} — the method's
   *       containing class is the binder itself.</li>
   *   <li><b>Kotlin extension / wrapper</b>: {@code mapBinder<K, V>()} — the containing
   *       class is unrelated, but the call's return type is the binder class.</li>
   * </ol>
   *
   * @param resolvedQName  the FQN of the method's containing class
   * @param call           the UAST call expression
   * @param binderFqn      the expected binder FQN (e.g., {@code "com.google.inject.multibindings.MapBinder"})
   */
  static boolean isBinderMethod(@NotNull String resolvedQName,
                                @NotNull UCallExpression call,
                                @NotNull String binderFqn) {
    // Direct match: method is declared on the binder class itself.
    if (binderFqn.equals(resolvedQName) || isGuicePackage(resolvedQName)) {
      return true;
    }

    // UAST return type: covers Kotlin extensions, wrapper functions, etc.
    PsiType returnType = call.getReturnType();
    if (returnType instanceof PsiClassType ct) {
      PsiClass returnClass = ct.resolve();
      if (returnClass != null) {
        String returnFqn = returnClass.getQualifiedName();
        if (binderFqn.equals(returnFqn) || (returnFqn != null && isGuicePackage(returnFqn))) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Checks whether the resolved method belongs to a binding builder class hierarchy,
   * handling Kotlin extension functions via UAST.
   *
   * <p>For Kotlin extensions like {@code LinkedBindingBuilder<T>.to()}, the containing
   * class is the file-level class.  We detect these by checking the call's UAST
   * receiver type, which resolves correctly for both Java qualified calls and Kotlin
   * extension calls.
   *
   * @param resolvedQName  the FQN of the method's containing class
   * @param call           the UAST call expression
   * @param containingClass the resolved method's containing class
   * @param builderFqn     the expected builder FQN (e.g., {@code "com.google.inject.binder.LinkedBindingBuilder"})
   */
  static boolean isBindingBuilderMethod(@NotNull String resolvedQName,
                                        @NotNull UCallExpression call,
                                        @NotNull PsiClass containingClass,
                                        @NotNull String builderFqn) {
    // Direct match: method is declared on the builder class or its subtype.
    if (builderFqn.equals(resolvedQName) ||
        InheritanceUtil.isInheritor(containingClass, builderFqn) ||
        isGuicePackage(resolvedQName)) {
      return true;
    }

    // UAST receiver type: covers both Java qualified calls and Kotlin extension calls.
    PsiType receiverType = call.getReceiverType();
    if (receiverType instanceof PsiClassType ct) {
      PsiClass receiverClass = ct.resolve();
      if (receiverClass != null &&
          (builderFqn.equals(receiverClass.getQualifiedName()) ||
           InheritanceUtil.isInheritor(receiverClass, builderFqn))) {
        return true;
      }
    }
    return false;
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
