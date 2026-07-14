// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor;

public abstract class BaseUastInspectionVisitor extends AbstractUastNonRecursiveVisitor {
  private final BaseUastInspection inspection;
  private final ProblemsHolder holder;
  private final boolean onTheFly;

  protected BaseUastInspectionVisitor(@NotNull BaseUastInspection inspection, @NotNull ProblemsHolder holder, boolean onTheFly) {
    this.inspection = inspection;
    this.holder = holder;
    this.onTheFly = onTheFly;
  }

  protected void registerCallError(@NotNull org.jetbrains.uast.UCallExpression expression, Object... infos) {
    final UElement methodId = expression.getMethodIdentifier();
    registerError(methodId != null ? methodId : expression, infos);
  }

  protected void registerClassError(@NotNull UClass aClass, Object... infos) {
    final UElement nameIdentifier = aClass.getUastAnchor();
    registerError(nameIdentifier != null ? nameIdentifier : aClass, infos);
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
