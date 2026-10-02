// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.BindToConstructorDescriptor;
import com.intellij.guice.model.beans.BindToDescriptor;
import com.intellij.guice.model.beans.BindToInstanceDescriptor;
import com.intellij.guice.model.beans.BindToProviderDescriptor;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
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

/**
 * Shared utility methods for {@link GuiceBindingContributor} implementations.
 *
 * <p>These were originally private helpers in
 * {@code com.intellij.guice.model.GuiceInjectorManager} and are extracted here
 * so that multiple contributors can share them.
 */
@ApiStatus.Experimental
public final class ContributorUtil {

  private ContributorUtil() {
  }

  @ApiStatus.Experimental
  @FunctionalInterface
  public interface DualTypeDescriptorFactory {
    @NotNull BindDescriptor create(@NotNull PsiElement source, @Nullable PsiType keyType, @Nullable PsiType valType);
  }

  /**
   * Checks whether a binder call chain or a local variable initialized by the binder call
   * invokes one of the given method names in the enclosing method.
   */
  public static boolean hasBinderCall(@NotNull BindDescriptor descriptor, String @NotNull ... methodNames) {
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
