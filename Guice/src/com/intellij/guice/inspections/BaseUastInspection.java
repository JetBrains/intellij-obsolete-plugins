// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.util.InspectionMessage;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.uast.UastHintedVisitorAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor;

public abstract class BaseUastInspection extends LocalInspectionTool {
  protected abstract @NotNull @InspectionMessage String buildErrorString(Object... infos);

  protected boolean buildQuickFixesOnlyForOnTheFlyErrors() {
    return false;
  }

  public abstract @NotNull AbstractUastNonRecursiveVisitor buildUastVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly);

  private final Class<? extends UElement>[] myUElementsTypesHint;

  @SafeVarargs
  protected BaseUastInspection(Class<? extends UElement>... uElementsTypesHint) {
    myUElementsTypesHint = uElementsTypesHint;
  }

  @Override
  public @NotNull PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, final boolean isOnTheFly) {
    return UastHintedVisitorAdapter.create(
      holder.getFile().getLanguage(),
      buildUastVisitor(holder, isOnTheFly),
      myUElementsTypesHint
    );
  }

  public LocalQuickFix buildFix(PsiElement location, Object[] infos) {
    return null;
  }
}
