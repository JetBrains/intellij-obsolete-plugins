// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UClassLiteralExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UQualifiedReferenceExpression;
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor;

import java.util.List;

public abstract class BaseUastInspectionVisitor extends AbstractUastNonRecursiveVisitor {
  private final BaseUastInspection inspection;
  private final ProblemsHolder holder;
  private final boolean onTheFly;

  protected BaseUastInspectionVisitor(@NotNull BaseUastInspection inspection, @NotNull ProblemsHolder holder, boolean onTheFly) {
    this.inspection = inspection;
    this.holder = holder;
    this.onTheFly = onTheFly;
  }

  protected void registerCallError(@NotNull UCallExpression expression, Object... infos) {
    final UElement methodId = expression.getMethodIdentifier();
    registerError(methodId != null ? methodId : expression, infos);
  }

  /**
   * Registers the problem on the class that a binding call names, for example {@code Impl} in {@code to(Impl.class)}
   * or in {@code to(Impl::class.java)}.
   * A Kotlin call can name the class with a type argument, for example {@code to<Impl>()}. Then the problem is on the call.
   * For a Java call, the source PSI of the call is the whole chain, so do not use the call when an argument exists.
   */
  protected void registerClassArgumentError(@NotNull UCallExpression call, Object... infos) {
    List<UExpression> args = call.getValueArguments();
    if (args.isEmpty()) {
      registerError(call, infos);
      return;
    }
    if (!registerClassLiteralError(args.getFirst(), infos)) {
      registerCallError(call, infos);
    }
  }

  /**
   * Registers the problem on the class name of a class literal, for example {@code Impl} in {@code Impl.class}
   * or in {@code Impl::class.java}. For another expression, registers the problem on the whole expression.
   *
   * @return {@code false} if the expression has no source PSI, so nothing was registered
   */
  protected boolean registerClassLiteralError(@NotNull UExpression expression, Object... infos) {
    UExpression arg = GuiceUtils.skipParenthesesAndCasts(expression);
    // Kotlin: Impl::class.java is a qualified expression with the class literal as the receiver.
    if (arg instanceof UQualifiedReferenceExpression qualified && qualified.getReceiver() instanceof UClassLiteralExpression literal) {
      arg = literal;
    }
    if (arg instanceof UClassLiteralExpression literal) {
      UExpression operand = literal.getExpression();
      PsiElement operandPsi = operand != null ? operand.getSourcePsi() : null;
      if (operandPsi != null) {
        registerError(operandPsi, infos);
        return true;
      }
    }
    PsiElement argPsi = arg != null ? arg.getSourcePsi() : null;
    if (argPsi == null) return false;
    registerError(argPsi, infos);
    return true;
  }

  protected void registerMethodError(@NotNull UMethod method, Object... infos) {
    final UElement nameIdentifier = method.getUastAnchor();
    registerError(nameIdentifier != null ? nameIdentifier : method, infos);
  }

  protected void registerError(@NotNull UElement location, Object... infos) {
    final PsiElement psi = location.getSourcePsi();
    if (psi != null) {
      registerError(psi, infos);
    }
  }

  protected void registerError(@NotNull PsiElement location, Object... infos) {
    final LocalQuickFix[] fixes = createFixes(location, infos);
    final String description = inspection.buildErrorString(infos);
    holder.registerProblem(location, description, fixes);
  }

  private @NotNull LocalQuickFix @Nullable [] createFixes(PsiElement location, Object[] infos) {
    if (!onTheFly && inspection.buildQuickFixesOnlyForOnTheFlyErrors()) {
      return null;
    }

    final LocalQuickFix fix = inspection.buildFix(location, infos);
    if (fix == null) {
      return null;
    }
    return new LocalQuickFix[]{fix};
  }
}
