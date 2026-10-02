// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UQualifiedReferenceExpression;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Declarative pattern that matches UAST call expressions in multiple filtering stages.
 *
 * <p>Callers build a pattern with {@link #named(String...)} and refine it with syntactic
 * argument bounds and semantic owner, receiver, or return-type constraints.
 */
@ApiStatus.Experimental
public final class GuiceCallPattern {
  private final @NotNull Set<String> myMethodNames;
  private final int myMinValueArguments;
  private final int myMaxValueArguments;
  private final boolean myRequireValueOrTypeArgument;
  private final boolean myStatementOnly;
  private final @NotNull Set<String> myExactOwnerClasses;
  private final @NotNull Set<String> myOwnerInheritors;
  private final @NotNull Set<String> myReceiverInheritors;
  private final @NotNull Set<String> myReturnInheritors;
  private final boolean myAllowGuicePackageOwner;
  private final boolean myMatchAnyResolvedOwner;
  private final boolean myAllowUnresolved;
  private final @NotNull Set<String> myUnresolvedChainHeads;

  private GuiceCallPattern(@NotNull Set<String> methodNames,
                           int minValueArguments,
                           int maxValueArguments,
                           boolean requireValueOrTypeArgument,
                           boolean statementOnly,
                           @NotNull Set<String> exactOwnerClasses,
                           @NotNull Set<String> ownerInheritors,
                           @NotNull Set<String> receiverInheritors,
                           @NotNull Set<String> returnInheritors,
                           boolean allowGuicePackageOwner,
                           boolean matchAnyResolvedOwner,
                           boolean allowUnresolved,
                           @NotNull Set<String> unresolvedChainHeads) {
    myMethodNames = methodNames;
    myMinValueArguments = minValueArguments;
    myMaxValueArguments = maxValueArguments;
    myRequireValueOrTypeArgument = requireValueOrTypeArgument;
    myStatementOnly = statementOnly;
    myExactOwnerClasses = exactOwnerClasses;
    myOwnerInheritors = ownerInheritors;
    myReceiverInheritors = receiverInheritors;
    myReturnInheritors = returnInheritors;
    myAllowGuicePackageOwner = allowGuicePackageOwner;
    myMatchAnyResolvedOwner = matchAnyResolvedOwner;
    myAllowUnresolved = allowUnresolved;
    myUnresolvedChainHeads = unresolvedChainHeads;
  }

  /**
   * Creates a pattern that matches calls with any of the given method names.
   */
  public static @NotNull GuiceCallPattern named(String @NotNull ... methodNames) {
    return new GuiceCallPattern(
        Set.of(methodNames),
        0,
        Integer.MAX_VALUE,
        false,
        false,
        Set.of(),
        Set.of(),
        Set.of(),
        Set.of(),
        false,
        false,
        false,
        Set.of()
    );
  }

  /**
   * Creates a pattern that matches calls with any of the given method names.
   */
  public static @NotNull GuiceCallPattern named(@NotNull Collection<String> methodNames) {
    return new GuiceCallPattern(
        Set.copyOf(methodNames),
        0,
        Integer.MAX_VALUE,
        false,
        false,
        Set.of(),
        Set.of(),
        Set.of(),
        Set.of(),
        false,
        false,
        false,
        Set.of()
    );
  }

  public @NotNull GuiceCallPattern minArguments(int minValueArguments) {
    return new GuiceCallPattern(
        myMethodNames, minValueArguments, myMaxValueArguments, myRequireValueOrTypeArgument,
        myStatementOnly, myExactOwnerClasses, myOwnerInheritors, myReceiverInheritors,
        myReturnInheritors, myAllowGuicePackageOwner, myMatchAnyResolvedOwner, myAllowUnresolved, myUnresolvedChainHeads);
  }

  public @NotNull GuiceCallPattern maxArguments(int maxValueArguments) {
    return new GuiceCallPattern(
        myMethodNames, myMinValueArguments, maxValueArguments, myRequireValueOrTypeArgument,
        myStatementOnly, myExactOwnerClasses, myOwnerInheritors, myReceiverInheritors,
        myReturnInheritors, myAllowGuicePackageOwner, myMatchAnyResolvedOwner, myAllowUnresolved, myUnresolvedChainHeads);
  }

  public @NotNull GuiceCallPattern argumentCount(int count) {
    return new GuiceCallPattern(
        myMethodNames, count, count, myRequireValueOrTypeArgument,
        myStatementOnly, myExactOwnerClasses, myOwnerInheritors, myReceiverInheritors,
        myReturnInheritors, myAllowGuicePackageOwner, myMatchAnyResolvedOwner, myAllowUnresolved, myUnresolvedChainHeads);
  }

  /**
   * Requires the call to have at least one value argument or at least one explicit type argument.
   */
  public @NotNull GuiceCallPattern requireValueOrTypeArgument() {
    return new GuiceCallPattern(
        myMethodNames, myMinValueArguments, myMaxValueArguments, true,
        myStatementOnly, myExactOwnerClasses, myOwnerInheritors, myReceiverInheritors,
        myReturnInheritors, myAllowGuicePackageOwner, myMatchAnyResolvedOwner, myAllowUnresolved, myUnresolvedChainHeads);
  }

  /**
   * Requires the call to stand as a complete statement expression.
   */
  public @NotNull GuiceCallPattern statementOnly() {
    return new GuiceCallPattern(
        myMethodNames, myMinValueArguments, myMaxValueArguments, myRequireValueOrTypeArgument,
        true, myExactOwnerClasses, myOwnerInheritors, myReceiverInheritors,
        myReturnInheritors, myAllowGuicePackageOwner, myMatchAnyResolvedOwner, myAllowUnresolved, myUnresolvedChainHeads);
  }

  /**
   * Matches when the declaring class of the resolved method has one of the given qualified names.
   */
  public @NotNull GuiceCallPattern withOwnerClass(String @NotNull ... ownerClassFqns) {
    return new GuiceCallPattern(
        myMethodNames, myMinValueArguments, myMaxValueArguments, myRequireValueOrTypeArgument,
        myStatementOnly, union(myExactOwnerClasses, ownerClassFqns), myOwnerInheritors, myReceiverInheritors,
        myReturnInheritors, myAllowGuicePackageOwner, myMatchAnyResolvedOwner, myAllowUnresolved, myUnresolvedChainHeads);
  }

  /**
   * Matches when the declaring class of the resolved method equals or inherits from one of the given classes.
   */
  public @NotNull GuiceCallPattern withOwnerInheritor(String @NotNull ... ownerClassFqns) {
    return new GuiceCallPattern(
        myMethodNames, myMinValueArguments, myMaxValueArguments, myRequireValueOrTypeArgument,
        myStatementOnly, myExactOwnerClasses, union(myOwnerInheritors, ownerClassFqns), myReceiverInheritors,
        myReturnInheritors, myAllowGuicePackageOwner, myMatchAnyResolvedOwner, myAllowUnresolved, myUnresolvedChainHeads);
  }

  /**
   * Matches when the receiver type of the call equals or inherits from one of the given classes.
   */
  public @NotNull GuiceCallPattern withReceiverInheritor(String @NotNull ... receiverClassFqns) {
    return new GuiceCallPattern(
        myMethodNames, myMinValueArguments, myMaxValueArguments, myRequireValueOrTypeArgument,
        myStatementOnly, myExactOwnerClasses, myOwnerInheritors, union(myReceiverInheritors, receiverClassFqns),
        myReturnInheritors, myAllowGuicePackageOwner, myMatchAnyResolvedOwner, myAllowUnresolved, myUnresolvedChainHeads);
  }

  /**
   * Matches when the return type of the call equals or inherits from one of the given classes.
   */
  public @NotNull GuiceCallPattern withReturnInheritor(String @NotNull ... returnClassFqns) {
    return new GuiceCallPattern(
        myMethodNames, myMinValueArguments, myMaxValueArguments, myRequireValueOrTypeArgument,
        myStatementOnly, myExactOwnerClasses, myOwnerInheritors, myReceiverInheritors,
        union(myReturnInheritors, returnClassFqns), myAllowGuicePackageOwner, myMatchAnyResolvedOwner, myAllowUnresolved, myUnresolvedChainHeads);
  }

  /**
   * Matches a factory method on a binder class or a Kotlin wrapper that returns the binder class.
   */
  public @NotNull GuiceCallPattern forBinder(@NotNull String binderClassFqn) {
    return withOwnerClass(binderClassFqn).withReturnInheritor(binderClassFqn);
  }

  /**
   * Matches a method on a binding builder hierarchy or a Kotlin extension on that builder type.
   */
  public @NotNull GuiceCallPattern forBindingBuilder(String @NotNull ... builderClassFqns) {
    return withOwnerInheritor(builderClassFqns).withReceiverInheritor(builderClassFqns);
  }

  /**
   * Matches when the declaring class belongs to the {@code com.google.inject} package hierarchy.
   */
  public @NotNull GuiceCallPattern inGuicePackage() {
    return new GuiceCallPattern(
        myMethodNames, myMinValueArguments, myMaxValueArguments, myRequireValueOrTypeArgument,
        myStatementOnly, myExactOwnerClasses, myOwnerInheritors, myReceiverInheritors,
        myReturnInheritors, true, myMatchAnyResolvedOwner, myAllowUnresolved, myUnresolvedChainHeads);
  }

  /**
   * Matches any resolved method regardless of declaring class.
   */
  @NotNull GuiceCallPattern matchAnyResolvedOwner() {
    return new GuiceCallPattern(
        myMethodNames, myMinValueArguments, myMaxValueArguments, myRequireValueOrTypeArgument,
        myStatementOnly, myExactOwnerClasses, myOwnerInheritors, myReceiverInheritors,
        myReturnInheritors, myAllowGuicePackageOwner, true, myAllowUnresolved, myUnresolvedChainHeads);
  }

  /**
   * Allows the pattern to match when the call cannot be resolved.
   */
  public @NotNull GuiceCallPattern allowUnresolved(String @NotNull ... requiredChainHeads) {
    return new GuiceCallPattern(
        myMethodNames, myMinValueArguments, myMaxValueArguments, myRequireValueOrTypeArgument,
        myStatementOnly, myExactOwnerClasses, myOwnerInheritors, myReceiverInheritors,
        myReturnInheritors, myAllowGuicePackageOwner, myMatchAnyResolvedOwner, true, union(myUnresolvedChainHeads, requiredChainHeads));
  }

  public @NotNull Set<String> getMethodNames() {
    return myMethodNames;
  }

  public int getMinValueArguments() {
    return myMinValueArguments;
  }

  public int getMaxValueArguments() {
    return myMaxValueArguments;
  }

  public @NotNull Set<String> getExactOwnerClasses() {
    return myExactOwnerClasses;
  }

  public boolean isAllowUnresolved() {
    return myAllowUnresolved;
  }

  /**
   * Returns {@code true} when this pattern requires resolving the target method.
   */
  public boolean requiresResolution() {
    return !myExactOwnerClasses.isEmpty()
        || !myOwnerInheritors.isEmpty()
        || !myReceiverInheritors.isEmpty()
        || !myReturnInheritors.isEmpty()
        || myAllowGuicePackageOwner
        || myMatchAnyResolvedOwner;
  }

  /**
   * Returns {@code true} when this pattern only checks exact owner class names after resolution.
   */
  public boolean isExactOwnerOnly() {
    return !myExactOwnerClasses.isEmpty()
        && myOwnerInheritors.isEmpty()
        && myReceiverInheritors.isEmpty()
        && myReturnInheritors.isEmpty()
        && !myAllowGuicePackageOwner
        && !myMatchAnyResolvedOwner;
  }

  /**
   * Evaluates syntactic constraints before method resolution.
   */
  public boolean matchesSyntactic(@NotNull UCallExpression call, int valueArgCount) {
    if (valueArgCount < myMinValueArguments || valueArgCount > myMaxValueArguments) {
      return false;
    }
    if (myRequireValueOrTypeArgument && valueArgCount == 0 && call.getTypeArgumentCount() == 0) {
      return false;
    }
    if (myStatementOnly && GuiceUtils.getStatementExpression(call) == null) {
      return false;
    }
    return true;
  }

  /**
   * Evaluates semantic constraints on a resolved call using memoized inheritance checks.
   */
  public boolean matchesResolved(@NotNull UCallExpression call,
                                 @NotNull String resolvedOwnerQName,
                                 @NotNull PsiClass containingClass,
                                 @NotNull Map<String, Boolean> ownerInheritorCache,
                                 @NotNull Map<String, Boolean> receiverInheritorCache,
                                 @NotNull Map<String, Boolean> returnInheritorCache) {
    if (!requiresResolution() || myMatchAnyResolvedOwner) {
      return true;
    }
    if (myExactOwnerClasses.contains(resolvedOwnerQName)) {
      return true;
    }
    if (myAllowGuicePackageOwner && resolvedOwnerQName.startsWith("com.google.inject.")) {
      return true;
    }
    for (String ownerFqn : myOwnerInheritors) {
      if (ownerFqn.equals(resolvedOwnerQName)) {
        return true;
      }
      if (ownerInheritorCache.computeIfAbsent(ownerFqn, fqn -> InheritanceUtil.isInheritor(containingClass, fqn))) {
        return true;
      }
    }
    if (!myReceiverInheritors.isEmpty()) {
      for (String receiverFqn : myReceiverInheritors) {
        if (receiverInheritorCache.computeIfAbsent(receiverFqn, fqn -> matchesTypeOrInheritor(call.getReceiverType(), fqn))) {
          return true;
        }
      }
    }
    if (!myReturnInheritors.isEmpty()) {
      for (String returnFqn : myReturnInheritors) {
        if (returnInheritorCache.computeIfAbsent(returnFqn, fqn -> matchesTypeOrInheritor(call.getReturnType(), fqn))) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Evaluates constraints when a call cannot be resolved.
   */
  public boolean matchesUnresolved(@NotNull UCallExpression call) {
    if (!myAllowUnresolved) {
      return false;
    }
    if (myUnresolvedChainHeads.isEmpty()) {
      return true;
    }
    UCallExpression outermost = ContributorUtil.getOutermostCall(call);
    return hasChainHead(outermost, myUnresolvedChainHeads);
  }

  private static boolean matchesTypeOrInheritor(@Nullable PsiType type, @NotNull String expectedFqn) {
    if (!(type instanceof PsiClassType classType)) {
      return false;
    }
    PsiClass resolved = classType.resolve();
    if (resolved == null) {
      return false;
    }
    return expectedFqn.equals(resolved.getQualifiedName()) || InheritanceUtil.isInheritor(resolved, expectedFqn);
  }

  private static boolean hasChainHead(@NotNull UCallExpression call, @NotNull Set<String> chainHeads) {
    UExpression current = call;
    while (current != null) {
      if (current instanceof UCallExpression callExpr) {
        String name = callExpr.getMethodName();
        if (name != null && chainHeads.contains(name)) {
          return true;
        }
        current = callExpr.getReceiver();
      }
      else if (current instanceof UQualifiedReferenceExpression qualifiedRef) {
        UExpression selector = qualifiedRef.getSelector();
        if (selector instanceof UCallExpression selectorCall) {
          String name = selectorCall.getMethodName();
          if (name != null && chainHeads.contains(name)) {
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

  private static @NotNull Set<String> union(@NotNull Set<String> existing, String @NotNull ... additions) {
    if (additions.length == 0) {
      return existing;
    }
    Set<String> merged = new LinkedHashSet<>(existing);
    Collections.addAll(merged, additions);
    return Set.copyOf(merged);
  }
}
