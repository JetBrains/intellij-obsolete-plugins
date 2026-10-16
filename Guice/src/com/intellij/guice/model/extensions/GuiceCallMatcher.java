// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UReferenceExpression;
import org.jetbrains.uast.UastCallKind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compiled multi-stage candidate filter for {@link GuiceCallPattern} rules.
 *
 * <p>All candidate rules for a method name are filtered together stage by stage:
 * <ol>
 *   <li>Stage 1 looks up the method-name bucket in {@code O(1)} time.</li>
 *   <li>Stage 2 checks syntactic argument bounds before method resolution.</li>
 *   <li>Stage 3 resolves the target method at most once for all surviving candidates.</li>
 *   <li>Stage 4 dispatches exact owner classes in {@code O(1)} time and shares memoized
 *       inheritance checks across remaining rules.</li>
 * </ol>
 *
 * @param <H> the handler type associated with each pattern
 */
@ApiStatus.Internal
public final class GuiceCallMatcher<H> {
  private final @NotNull Map<String, MethodBucket<H>> myBuckets;
  private final @NotNull Set<String> myMethodNames;
  private final boolean myStopOnFirstMatch;
  private final boolean myHasGlobalRules;

  @FunctionalInterface
  public interface HandlerInvoker<H, C> {
    boolean invoke(@NotNull H handler,
                   @NotNull UCallExpression call,
                   @Nullable String resolvedOwnerQName,
                   @Nullable PsiClass containingClass,
                   @NotNull C context);
  }

  private record Rule<H>(@NotNull GuiceCallPattern pattern, @NotNull H handler, boolean moduleOnly) {
    boolean isApplicable(boolean isModule) {
      return isModule || !moduleOnly;
    }
  }

  private record MethodBucket<H>(int minArity,
                                 int maxArity,
                                 boolean anyRequiresResolution,
                                 boolean hasGlobalRules,
                                 @NotNull List<Rule<H>> allRules,
                                 @NotNull Map<String, List<Rule<H>>> byExactOwnerClass,
                                 @NotNull List<Rule<H>> generalRules,
                                 @NotNull List<Rule<H>> unresolvedRules) {
    boolean hasSyntacticCandidate(@NotNull UCallExpression call, int argCount, boolean isModule) {
      for (Rule<H> rule : allRules) {
        if (rule.isApplicable(isModule) && rule.pattern.matchesSyntactic(call, argCount)) {
          return true;
        }
      }
      return false;
    }
  }

  private GuiceCallMatcher(@NotNull Map<String, MethodBucket<H>> buckets,
                           @NotNull Set<String> methodNames,
                           boolean stopOnFirstMatch,
                           boolean hasGlobalRules) {
    myBuckets = buckets;
    myMethodNames = methodNames;
    myStopOnFirstMatch = stopOnFirstMatch;
    myHasGlobalRules = hasGlobalRules;
  }

  public static <H> @NotNull Builder<H> builder(boolean stopOnFirstMatch) {
    return new Builder<>(stopOnFirstMatch);
  }

  public boolean isEmpty() {
    return myBuckets.isEmpty();
  }

  public boolean hasGlobalRules() {
    return myHasGlobalRules;
  }

  public @NotNull Set<String> getMethodNames() {
    return myMethodNames;
  }

  public <C> boolean process(@NotNull UCallExpression call,
                             @NotNull C context,
                             @NotNull HandlerInvoker<H, C> invoker) {
    return process(call, true, context, invoker);
  }

  public static @Nullable String getCallLookupName(@NotNull UCallExpression call) {
    String methodName = call.getMethodName();
    if (methodName != null && !"<init>".equals(methodName)) {
      return methodName;
    }
    if (call.getKind() == UastCallKind.CONSTRUCTOR_CALL) {
      UReferenceExpression classRef = call.getClassReference();
      if (classRef != null) {
        String resolvedName = classRef.getResolvedName();
        if (resolvedName != null) {
          return resolvedName;
        }
        PsiElement sourcePsi = classRef.getSourcePsi();
        if (sourcePsi instanceof PsiJavaCodeReferenceElement javaRef) {
          return javaRef.getReferenceName();
        }
      }
    }
    return methodName;
  }

  public <C> boolean process(@NotNull UCallExpression call,
                             boolean isModule,
                             @NotNull C context,
                             @NotNull HandlerInvoker<H, C> invoker) {
    // Stage 1: O(1) method-name bucket lookup.
    String methodName = getCallLookupName(call);
    if (methodName == null) {
      return false;
    }
    MethodBucket<H> bucket = myBuckets.get(methodName);
    if (bucket == null || (!isModule && !bucket.hasGlobalRules)) {
      return false;
    }

    // Stage 2: Bucket-level and rule-level syntactic checks before resolution.
    int argCount = call.getValueArgumentCount();
    if (argCount < bucket.minArity || argCount > bucket.maxArity) {
      return false;
    }
    if (!bucket.hasSyntacticCandidate(call, argCount, isModule)) {
      return false;
    }

    // Fast path when no rule in this bucket requires method resolution.
    if (!bucket.anyRequiresResolution) {
      boolean matched = false;
      for (Rule<H> rule : bucket.generalRules) {
        if (rule.isApplicable(isModule) && rule.pattern.matchesSyntactic(call, argCount)) {
          if (invoker.invoke(rule.handler, call, null, null, context)) {
            matched = true;
            if (myStopOnFirstMatch) {
              return true;
            }
          }
        }
      }
      return matched;
    }

    // Stage 3: Resolve the target method once for all candidates in the bucket.
    PsiMethod resolved = call.resolve();
    PsiClass containingClass = resolved != null ? resolved.getContainingClass() : null;
    String resolvedOwnerQName = containingClass != null ? containingClass.getQualifiedName() : null;

    boolean matched = false;

    // Stage 4a: Resolved call dispatch (O(1) exact owner lookup + memoized general rules).
    if (containingClass != null && resolvedOwnerQName != null) {
      List<Rule<H>> exactRules = bucket.byExactOwnerClass.get(resolvedOwnerQName);
      if (exactRules != null) {
        for (Rule<H> rule : exactRules) {
          if (rule.isApplicable(isModule) && rule.pattern.matchesSyntactic(call, argCount)) {
            if (invoker.invoke(rule.handler, call, resolvedOwnerQName, containingClass, context)) {
              matched = true;
              if (myStopOnFirstMatch) {
                return true;
              }
            }
          }
        }
      }

      if (!bucket.generalRules.isEmpty()) {
        Map<String, Boolean> ownerCache = new HashMap<>(4);
        Map<String, Boolean> receiverCache = new HashMap<>(4);
        Map<String, Boolean> returnCache = new HashMap<>(4);
        for (Rule<H> rule : bucket.generalRules) {
          if (rule.isApplicable(isModule)
              && rule.pattern.matchesSyntactic(call, argCount)
              && rule.pattern.matchesResolved(
                  call, resolvedOwnerQName, containingClass, ownerCache, receiverCache, returnCache)) {
            if (invoker.invoke(rule.handler, call, resolvedOwnerQName, containingClass, context)) {
              matched = true;
              if (myStopOnFirstMatch) {
                return true;
              }
            }
          }
        }
      }
      return matched;
    }

    // Stage 4b: Unresolved call fallback.
    if (!bucket.unresolvedRules.isEmpty()) {
      for (Rule<H> rule : bucket.unresolvedRules) {
        if (rule.isApplicable(isModule)
            && rule.pattern.matchesSyntactic(call, argCount)
            && (!rule.pattern.requiresResolution() || rule.pattern.matchesUnresolved(call))) {
          if (invoker.invoke(rule.handler, call, null, null, context)) {
            matched = true;
            if (myStopOnFirstMatch) {
              return true;
            }
          }
        }
      }
    }
    return matched;
  }

  public static final class Builder<H> {
    private final boolean myStopOnFirstMatch;
    private final Map<String, List<Rule<H>>> myRulesByMethod = new HashMap<>();
    private final Set<String> myMethodNames = new LinkedHashSet<>();

    private Builder(boolean stopOnFirstMatch) {
      myStopOnFirstMatch = stopOnFirstMatch;
    }

    public @NotNull Builder<H> add(@NotNull GuiceCallPattern pattern, @NotNull H handler) {
      return add(pattern, handler, false);
    }

    public @NotNull Builder<H> add(@NotNull GuiceCallPattern pattern, @NotNull H handler, boolean moduleOnly) {
      Rule<H> rule = new Rule<>(pattern, handler, moduleOnly);
      for (String methodName : pattern.getMethodNames()) {
        myMethodNames.add(methodName);
        myRulesByMethod.computeIfAbsent(methodName, _k -> new ArrayList<>()).add(rule);
      }
      return this;
    }

    public @NotNull GuiceCallMatcher<H> build() {
      Map<String, MethodBucket<H>> buckets = new HashMap<>(myRulesByMethod.size());
      boolean hasGlobalRules = false;
      for (Map.Entry<String, List<Rule<H>>> entry : myRulesByMethod.entrySet()) {
        MethodBucket<H> bucket = compileBucket(entry.getValue());
        if (bucket.hasGlobalRules) {
          hasGlobalRules = true;
        }
        buckets.put(entry.getKey(), bucket);
      }
      return new GuiceCallMatcher<>(
          Map.copyOf(buckets), Set.copyOf(myMethodNames), myStopOnFirstMatch, hasGlobalRules);
    }

    private static <H> @NotNull MethodBucket<H> compileBucket(@NotNull List<Rule<H>> rules) {
      int minArity = Integer.MAX_VALUE;
      int maxArity = 0;
      boolean anyRequiresResolution = false;
      boolean hasGlobalRules = false;
      Map<String, List<Rule<H>>> byExactOwner = new HashMap<>();
      List<Rule<H>> generalRules = new ArrayList<>();
      List<Rule<H>> unresolvedRules = new ArrayList<>();

      for (Rule<H> rule : rules) {
        GuiceCallPattern pattern = rule.pattern;
        minArity = Math.min(minArity, pattern.getMinValueArguments());
        maxArity = Math.max(maxArity, pattern.getMaxValueArguments());
        if (pattern.requiresResolution()) {
          anyRequiresResolution = true;
        }
        if (!rule.moduleOnly) {
          hasGlobalRules = true;
        }
        if (pattern.isExactOwnerOnly()) {
          for (String exactOwner : pattern.getExactOwnerClasses()) {
            byExactOwner.computeIfAbsent(exactOwner, _k -> new ArrayList<>()).add(rule);
          }
        }
        else {
          generalRules.add(rule);
        }
        if (!pattern.requiresResolution() || pattern.isAllowUnresolved()) {
          unresolvedRules.add(rule);
        }
      }

      if (minArity > maxArity) {
        minArity = 0;
        maxArity = Integer.MAX_VALUE;
      }

      Map<String, List<Rule<H>>> immutableExact = new HashMap<>(byExactOwner.size());
      for (Map.Entry<String, List<Rule<H>>> entry : byExactOwner.entrySet()) {
        immutableExact.put(entry.getKey(), List.copyOf(entry.getValue()));
      }

      return new MethodBucket<>(
          minArity,
          maxArity,
          anyRequiresResolution,
          hasGlobalRules,
          List.copyOf(rules),
          Map.copyOf(immutableExact),
          List.copyOf(generalRules),
          List.copyOf(unresolvedRules)
      );
    }
  }
}
