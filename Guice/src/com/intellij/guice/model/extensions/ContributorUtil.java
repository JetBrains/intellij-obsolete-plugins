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
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.ULocalVariable;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UResolvable;
import org.jetbrains.uast.UastUtils;
import org.jetbrains.uast.visitor.AbstractUastVisitor;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Shared utility methods for {@link GuiceBindingContributor} implementations.
 *
 * <p>These were originally private helpers in
 * {@code com.intellij.guice.model.GuiceInjectorManager} and are extracted here
 * so that multiple contributors can share them.
 */
@ApiStatus.Internal
public final class ContributorUtil {

  private ContributorUtil() {
  }

  @FunctionalInterface
  interface DualTypeDescriptorFactory {
    @NotNull BindDescriptor create(@NotNull PsiElement source, @Nullable PsiType keyType, @Nullable PsiType valType);
  }

  static boolean processSingleTypeBinderCall(@NotNull UCallExpression call,
                                             @NotNull String resolvedQName,
                                             @NotNull String binderFqn,
                                             @NotNull Set<BindDescriptor> descriptors,
                                             @NotNull BiFunction<? super PsiElement, ? super PsiType, ? extends BindDescriptor> factory) {
    if (!isBinderMethod(resolvedQName, call, binderFqn)) {
      return false;
    }
    PsiElement outermostSource = getOutermostSource(call);
    if (outermostSource != null) {
      descriptors.add(factory.apply(outermostSource, extractSinglePsiType(call)));
      return true;
    }
    return false;
  }

  static boolean processDualTypeBinderCall(@NotNull UCallExpression call,
                                           @NotNull String resolvedQName,
                                           @NotNull String binderFqn,
                                           @NotNull Set<BindDescriptor> descriptors,
                                           @NotNull DualTypeDescriptorFactory factory) {
    if (!isBinderMethod(resolvedQName, call, binderFqn)) {
      return false;
    }
    PsiElement outermostSource = getOutermostSource(call);
    if (outermostSource != null) {
      PsiType[] kv = extractDualPsiTypes(call);
      descriptors.add(factory.create(outermostSource, kv[0], kv[1]));
      return true;
    }
    return false;
  }

  /**
   * Checks whether a binder call chain or a local variable initialized by the binder call
   * invokes one of the given method names in the enclosing method.
   */
  static boolean hasBinderCall(@NotNull BindDescriptor descriptor, String @NotNull ... methodNames) {
    UCallExpression outermost = descriptor.getOutermostCall();
    if (outermost == null) return false;
    for (String methodName : methodNames) {
      if (GuiceUtils.findCallInChain(outermost, methodName) != null) {
        return true;
      }
    }
    ULocalVariable localVar = UastUtils.getParentOfType(outermost, ULocalVariable.class);
    if (localVar == null || !(localVar.getJavaPsi() instanceof PsiVariable targetVar)) {
      return false;
    }
    UMethod enclosingMethod = UastUtils.getParentOfType(localVar, UMethod.class);
    if (enclosingMethod == null) return false;

    Set<String> names = Set.of(methodNames);
    boolean[] found = {false};
    enclosingMethod.accept(new AbstractUastVisitor() {
      @Override
      public boolean visitCallExpression(@NotNull UCallExpression node) {
        if (found[0]) return true;
        String name = node.getMethodName();
        if (name != null && names.contains(name)) {
          UExpression receiver = GuiceUtils.skipParenthesesAndCasts(GuiceUtils.getEffectiveReceiver(node));
          if (receiver instanceof UResolvable resolvable && targetVar.equals(resolvable.resolve())) {
            found[0] = true;
            return true;
          }
        }
        return super.visitCallExpression(node);
      }
    });
    return found[0];
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
  public static boolean isBinderMethod(@NotNull String resolvedQName,
                                       @NotNull UCallExpression call,
                                       @NotNull String binderFqn) {
    // Direct match: method is declared on the binder class itself.
    // A method of another Guice class, for example ThrowingProviderBinder, does not match.
    if (binderFqn.equals(resolvedQName)) {
      return true;
    }

    // UAST return type: covers Kotlin extensions, wrapper functions, etc.
    PsiType returnType = call.getReturnType();
    if (returnType instanceof PsiClassType ct) {
      PsiClass returnClass = ct.resolve();
      if (returnClass != null) {
        String returnFqn = returnClass.getQualifiedName();
        if (binderFqn.equals(returnFqn) || InheritanceUtil.isInheritor(returnClass, binderFqn)) {
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
  public static boolean isBindingBuilderMethod(@NotNull String resolvedQName,
                                               @NotNull UCallExpression call,
                                               @NotNull PsiClass containingClass,
                                               @NotNull String builderFqn) {
    // Direct match: method is declared on the builder class or its subtype.
    if (builderFqn.equals(resolvedQName) ||
        InheritanceUtil.isInheritor(containingClass, builderFqn)) {
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
  public static boolean createBindingTailDescriptor(@NotNull String methodName,
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
  public static @Nullable PsiElement getOutermostSource(@NotNull UCallExpression call) {
    return GuiceUtils.getOutermostQualifiedParent(call).getSourcePsi();
  }

  /**
   * Returns the outermost call in a chained qualified expression, or the original call
   * if it is not part of a chain.
   */
  public static @NotNull UCallExpression getOutermostCall(@NotNull UCallExpression bindCall) {
    UElement outermost = GuiceUtils.getOutermostQualifiedParent(bindCall);
    UExpression selector = GuiceUtils.getSelectorIfQualified(outermost);
    return selector instanceof UCallExpression ? (UCallExpression)selector : bindCall;
  }

  /**
   * Extracts a single type argument from a factory or binder call expression.
   * Tries explicit type arguments first, then checks the second value argument
   * (when the first argument is a binder reference), and falls back to the first
   * value argument for single-argument calls.
   *
   * @return the resolved {@link PsiType}, or {@code null} if unavailable
   */
  public static @Nullable PsiType extractSinglePsiType(@NotNull UCallExpression call) {
    List<PsiType> typeArgs = call.getTypeArguments();
    if (!typeArgs.isEmpty()) {
      return typeArgs.getFirst();
    }
    List<UExpression> args = call.getValueArguments();
    if (args.size() >= 2) {
      return GuiceUtils.getBindingTypeFromExpression(args.get(1));
    }
    if (args.size() == 1) {
      return GuiceUtils.getBindingTypeFromExpression(args.getFirst());
    }
    return null;
  }

  public static @Nullable PsiClass extractSingleTypeArg(@NotNull UCallExpression call) {
    PsiType type = extractSinglePsiType(call);
    return type instanceof PsiClassType ct ? ct.resolve() : null;
  }

  public static PsiType @NotNull [] extractDualPsiTypes(@NotNull UCallExpression call) {
    PsiType keyType = null;
    PsiType valType = null;
    List<PsiType> typeArgs = call.getTypeArguments();
    if (typeArgs.size() > 1) {
      keyType = typeArgs.getFirst();
      valType = typeArgs.get(1);
    }
    else {
      List<UExpression> args = call.getValueArguments();
      if (args.size() > 2) {
        keyType = GuiceUtils.getBindingTypeFromExpression(args.get(1));
        valType = GuiceUtils.getBindingTypeFromExpression(args.get(2));
      }
    }
    return new PsiType[]{keyType, valType};
  }
}
