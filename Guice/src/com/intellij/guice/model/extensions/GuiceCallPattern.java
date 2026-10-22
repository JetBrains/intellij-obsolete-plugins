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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Declarative pattern that matches UAST call expressions in multiple filtering stages.
 *
 * <p>Callers build a pattern with {@link #named(String...)} and refine it with syntactic
 * argument bounds and semantic owner, receiver, or return-type constraints.
 *
 * <h3>Semantics of the constraints</h3>
 * <ul>
 *   <li>Constraints of different kinds are <b>conjunctive</b>: {@code withOwnerClass(X).withReturnInheritor(Y)}
 *       matches a call that is declared in {@code X} <b>and</b> returns a {@code Y}.</li>
 *   <li>Within one kind, the listed classes are alternatives: {@code withOwnerClass(X, Z)} matches a call
 *       declared in {@code X} or in {@code Z}. {@link #withOwnerClass}, {@link #withOwnerInheritor} and
 *       {@link #inGuicePackage} are one kind, the owner.</li>
 *   <li>{@link #anyOf} combines whole patterns as alternatives, for example a factory method on a binder
 *       class or a wrapper function that returns the binder class ({@link #forBinder}).</li>
 *   <li>A constraint added to a combined pattern applies to every alternative.</li>
 * </ul>
 */
@ApiStatus.Experimental
public final class GuiceCallPattern {

  /**
   * One conjunction of owner, receiver, and return-type constraints.
   */
  private record Semantic(@NotNull Set<String> exactOwnerClasses,
                          @NotNull Set<String> ownerInheritors,
                          boolean allowGuicePackageOwner,
                          @NotNull Set<String> receiverInheritors,
                          @NotNull Set<String> returnInheritors) {
    static final Semantic ANY = new Semantic(Set.of(), Set.of(), false, Set.of(), Set.of());

    boolean hasOwnerConstraint() {
      return !exactOwnerClasses.isEmpty() || !ownerInheritors.isEmpty() || allowGuicePackageOwner;
    }

    boolean requiresResolution() {
      return hasOwnerConstraint() || !receiverInheritors.isEmpty() || !returnInheritors.isEmpty();
    }

    boolean isExactOwnerOnly() {
      return !exactOwnerClasses.isEmpty()
             && ownerInheritors.isEmpty()
             && !allowGuicePackageOwner
             && receiverInheritors.isEmpty()
             && returnInheritors.isEmpty();
    }

    boolean matches(@NotNull UCallExpression call,
                    @Nullable String resolvedOwnerQName,
                    @Nullable PsiClass containingClass,
                    @NotNull Map<String, Boolean> ownerInheritorCache,
                    @NotNull Map<String, Boolean> receiverInheritorCache,
                    @NotNull Map<String, Boolean> returnInheritorCache) {
      if (hasOwnerConstraint() && !matchesOwner(resolvedOwnerQName, containingClass, ownerInheritorCache)) {
        return false;
      }
      if (!receiverInheritors.isEmpty()
          && !matchesAnyType(receiverInheritors, call.getReceiverType(), receiverInheritorCache)) {
        return false;
      }
      return returnInheritors.isEmpty()
             || matchesAnyType(returnInheritors, call.getReturnType(), returnInheritorCache);
    }

    private boolean matchesOwner(@Nullable String resolvedOwnerQName,
                                 @Nullable PsiClass containingClass,
                                 @NotNull Map<String, Boolean> ownerInheritorCache) {
      if (resolvedOwnerQName != null) {
        if (exactOwnerClasses.contains(resolvedOwnerQName)) return true;
        if (allowGuicePackageOwner && resolvedOwnerQName.startsWith("com.google.inject.")) return true;
        if (ownerInheritors.contains(resolvedOwnerQName)) return true;
      }
      if (containingClass != null) {
        for (String ownerFqn : ownerInheritors) {
          if (ownerInheritorCache.computeIfAbsent(ownerFqn, fqn -> InheritanceUtil.isInheritor(containingClass, fqn))) {
            return true;
          }
        }
      }
      return false;
    }

    private static boolean matchesAnyType(@NotNull Set<String> expectedFqns,
                                          @Nullable PsiType type,
                                          @NotNull Map<String, Boolean> cache) {
      for (String expectedFqn : expectedFqns) {
        if (cache.computeIfAbsent(expectedFqn, fqn -> matchesTypeOrInheritor(type, fqn))) {
          return true;
        }
      }
      return false;
    }
  }

  private final @NotNull Set<String> myMethodNames;
  private final int myMinValueArguments;
  private final int myMaxValueArguments;
  private final boolean myRequireValueOrTypeArgument;
  private final boolean myStatementOnly;
  /** Alternatives: the pattern matches when any of them matches. Never empty. */
  private final @NotNull List<Semantic> myAlternatives;
  private final boolean myAllowUnresolved;
  private final @NotNull Set<String> myUnresolvedChainHeads;

  private GuiceCallPattern(@NotNull Set<String> methodNames,
                           int minValueArguments,
                           int maxValueArguments,
                           boolean requireValueOrTypeArgument,
                           boolean statementOnly,
                           @NotNull List<Semantic> alternatives,
                           boolean allowUnresolved,
                           @NotNull Set<String> unresolvedChainHeads) {
    if (minValueArguments < 0 || minValueArguments > maxValueArguments) {
      throw new IllegalArgumentException("Invalid argument bounds: " + minValueArguments + ".." + maxValueArguments);
    }
    myMethodNames = methodNames;
    myMinValueArguments = minValueArguments;
    myMaxValueArguments = maxValueArguments;
    myRequireValueOrTypeArgument = requireValueOrTypeArgument;
    myStatementOnly = statementOnly;
    myAlternatives = alternatives;
    myAllowUnresolved = allowUnresolved;
    myUnresolvedChainHeads = unresolvedChainHeads;
  }

  /**
   * Creates a pattern that matches calls with any of the given method names.
   */
  public static @NotNull GuiceCallPattern named(String @NotNull ... methodNames) {
    return named(Arrays.asList(methodNames));
  }

  /**
   * Creates a pattern that matches calls with any of the given method names.
   */
  public static @NotNull GuiceCallPattern named(@NotNull Collection<String> methodNames) {
    if (methodNames.isEmpty()) {
      throw new IllegalArgumentException("A call pattern needs at least one method name");
    }
    return new GuiceCallPattern(
        Set.copyOf(new LinkedHashSet<>(methodNames)),
        0,
        Integer.MAX_VALUE,
        false,
        false,
        List.of(Semantic.ANY),
        false,
        Set.of()
    );
  }

  /**
   * Combines patterns as alternatives: the result matches a call when any of the patterns matches it.
   * The patterns must share the method names and the syntactic constraints.
   */
  public static @NotNull GuiceCallPattern anyOf(@NotNull GuiceCallPattern first, GuiceCallPattern @NotNull ... others) {
    List<Semantic> alternatives = new ArrayList<>(first.myAlternatives);
    boolean allowUnresolved = first.myAllowUnresolved;
    Set<String> chainHeads = new LinkedHashSet<>(first.myUnresolvedChainHeads);
    for (GuiceCallPattern other : others) {
      if (!other.myMethodNames.equals(first.myMethodNames)
          || other.myMinValueArguments != first.myMinValueArguments
          || other.myMaxValueArguments != first.myMaxValueArguments
          || other.myRequireValueOrTypeArgument != first.myRequireValueOrTypeArgument
          || other.myStatementOnly != first.myStatementOnly) {
        throw new IllegalArgumentException("anyOf() needs patterns with the same method names and syntactic constraints");
      }
      alternatives.addAll(other.myAlternatives);
      allowUnresolved |= other.myAllowUnresolved;
      chainHeads.addAll(other.myUnresolvedChainHeads);
    }
    return new GuiceCallPattern(
        first.myMethodNames, first.myMinValueArguments, first.myMaxValueArguments, first.myRequireValueOrTypeArgument,
        first.myStatementOnly, List.copyOf(alternatives), allowUnresolved, Set.copyOf(chainHeads));
  }

  private @NotNull GuiceCallPattern withSyntax(int minValueArguments,
                                               int maxValueArguments,
                                               boolean requireValueOrTypeArgument,
                                               boolean statementOnly) {
    return new GuiceCallPattern(
        myMethodNames, minValueArguments, maxValueArguments, requireValueOrTypeArgument,
        statementOnly, myAlternatives, myAllowUnresolved, myUnresolvedChainHeads);
  }

  private @NotNull GuiceCallPattern mapAlternatives(@NotNull UnaryOperator<Semantic> operator) {
    List<Semantic> mapped = new ArrayList<>(myAlternatives.size());
    for (Semantic alternative : myAlternatives) {
      mapped.add(operator.apply(alternative));
    }
    return new GuiceCallPattern(
        myMethodNames, myMinValueArguments, myMaxValueArguments, myRequireValueOrTypeArgument,
        myStatementOnly, List.copyOf(mapped), myAllowUnresolved, myUnresolvedChainHeads);
  }

  public @NotNull GuiceCallPattern minArguments(int minValueArguments) {
    return withSyntax(minValueArguments, myMaxValueArguments, myRequireValueOrTypeArgument, myStatementOnly);
  }

  public @NotNull GuiceCallPattern maxArguments(int maxValueArguments) {
    return withSyntax(myMinValueArguments, maxValueArguments, myRequireValueOrTypeArgument, myStatementOnly);
  }

  public @NotNull GuiceCallPattern argumentCount(int count) {
    return withSyntax(count, count, myRequireValueOrTypeArgument, myStatementOnly);
  }

  /**
   * Requires the call to have at least one value argument or at least one explicit type argument.
   */
  public @NotNull GuiceCallPattern requireValueOrTypeArgument() {
    return withSyntax(myMinValueArguments, myMaxValueArguments, true, myStatementOnly);
  }

  /**
   * Requires the call to stand as a complete statement expression.
   */
  public @NotNull GuiceCallPattern statementOnly() {
    return withSyntax(myMinValueArguments, myMaxValueArguments, myRequireValueOrTypeArgument, true);
  }

  /**
   * Requires the declaring class of the resolved method to have one of the given qualified names.
   */
  public @NotNull GuiceCallPattern withOwnerClass(String @NotNull ... ownerClassFqns) {
    return mapAlternatives(s -> new Semantic(union(s.exactOwnerClasses, ownerClassFqns), s.ownerInheritors,
                                             s.allowGuicePackageOwner, s.receiverInheritors, s.returnInheritors));
  }

  /**
   * Requires the declaring class of the resolved method to equal or inherit from one of the given classes.
   */
  public @NotNull GuiceCallPattern withOwnerInheritor(String @NotNull ... ownerClassFqns) {
    return mapAlternatives(s -> new Semantic(s.exactOwnerClasses, union(s.ownerInheritors, ownerClassFqns),
                                             s.allowGuicePackageOwner, s.receiverInheritors, s.returnInheritors));
  }

  /**
   * Requires the receiver type of the call to equal or inherit from one of the given classes.
   */
  public @NotNull GuiceCallPattern withReceiverInheritor(String @NotNull ... receiverClassFqns) {
    return mapAlternatives(s -> new Semantic(s.exactOwnerClasses, s.ownerInheritors, s.allowGuicePackageOwner,
                                             union(s.receiverInheritors, receiverClassFqns), s.returnInheritors));
  }

  /**
   * Requires the return type of the call to equal or inherit from one of the given classes.
   */
  public @NotNull GuiceCallPattern withReturnInheritor(String @NotNull ... returnClassFqns) {
    return mapAlternatives(s -> new Semantic(s.exactOwnerClasses, s.ownerInheritors, s.allowGuicePackageOwner,
                                             s.receiverInheritors, union(s.returnInheritors, returnClassFqns)));
  }

  /**
   * Matches a factory method declared on a binder class, or a wrapper (for example a Kotlin function)
   * that returns the binder class.
   */
  public @NotNull GuiceCallPattern forBinder(@NotNull String binderClassFqn) {
    return anyOf(withOwnerClass(binderClassFqn), withReturnInheritor(binderClassFqn));
  }

  /**
   * Matches a method declared in a binding builder hierarchy, or an extension (for example a Kotlin function)
   * whose receiver is such a builder.
   */
  public @NotNull GuiceCallPattern forBindingBuilder(String @NotNull ... builderClassFqns) {
    return anyOf(withOwnerInheritor(builderClassFqns), withReceiverInheritor(builderClassFqns));
  }

  /**
   * Requires the declaring class to belong to the {@code com.google.inject} package hierarchy.
   */
  public @NotNull GuiceCallPattern inGuicePackage() {
    return mapAlternatives(s -> new Semantic(s.exactOwnerClasses, s.ownerInheritors, true,
                                             s.receiverInheritors, s.returnInheritors));
  }

  /**
   * Allows the pattern to match when the call cannot be resolved, if the chain of the call
   * contains one of the given method names (or unconditionally when no names are given).
   */
  public @NotNull GuiceCallPattern allowUnresolved(String @NotNull ... requiredChainHeads) {
    return new GuiceCallPattern(
        myMethodNames, myMinValueArguments, myMaxValueArguments, myRequireValueOrTypeArgument,
        myStatementOnly, myAlternatives, true, union(myUnresolvedChainHeads, requiredChainHeads));
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

  /**
   * Returns the exact owner classes of all alternatives.
   */
  public @NotNull Set<String> getExactOwnerClasses() {
    if (myAlternatives.size() == 1) return myAlternatives.getFirst().exactOwnerClasses;
    Set<String> result = new LinkedHashSet<>();
    for (Semantic alternative : myAlternatives) {
      result.addAll(alternative.exactOwnerClasses);
    }
    return Set.copyOf(result);
  }

  @ApiStatus.Internal
  public boolean isAllowUnresolved() {
    return myAllowUnresolved;
  }

  /**
   * Returns {@code true} when this pattern requires resolving the target method.
   */
  @ApiStatus.Internal
  public boolean requiresResolution() {
    for (Semantic alternative : myAlternatives) {
      if (alternative.requiresResolution()) return true;
    }
    return false;
  }

  /**
   * Returns {@code true} when every alternative only checks exact owner class names after resolution,
   * so the matcher can dispatch the pattern by the owner name.
   */
  @ApiStatus.Internal
  public boolean isExactOwnerOnly() {
    for (Semantic alternative : myAlternatives) {
      if (!alternative.isExactOwnerOnly()) return false;
    }
    return true;
  }

  /**
   * Evaluates syntactic constraints before method resolution.
   */
  @ApiStatus.Internal
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
   * Evaluates the semantic constraints on a resolved call using memoized inheritance checks.
   *
   * @param resolvedOwnerQName the qualified name of the declaring class, or {@code null} when it has none
   *                           (a local or anonymous class); such an owner matches no owner constraint
   * @param containingClass    the declaring class, or {@code null} when the resolved method has none
   */
  @ApiStatus.Internal
  public boolean matchesResolved(@NotNull UCallExpression call,
                                 @Nullable String resolvedOwnerQName,
                                 @Nullable PsiClass containingClass,
                                 @NotNull Map<String, Boolean> ownerInheritorCache,
                                 @NotNull Map<String, Boolean> receiverInheritorCache,
                                 @NotNull Map<String, Boolean> returnInheritorCache) {
    for (Semantic alternative : myAlternatives) {
      if (alternative.matches(call, resolvedOwnerQName, containingClass,
                              ownerInheritorCache, receiverInheritorCache, returnInheritorCache)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Evaluates constraints when a call cannot be resolved.
   */
  @ApiStatus.Internal
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
