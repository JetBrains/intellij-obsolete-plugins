// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.util.InspectionMessage;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public abstract class BaseInspection extends LocalInspectionTool {
  protected abstract @NotNull @InspectionMessage String buildErrorString(Object... infos);

  protected boolean buildQuickFixesOnlyForOnTheFlyErrors() {
    return false;
  }

  public abstract BaseInspectionVisitor buildVisitor();

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    BaseInspectionVisitor visitor = buildVisitor();

    visitor.setProblemsHolder(holder);
    visitor.setOnTheFly(isOnTheFly);
    visitor.setInspection(this);
    return visitor;
  }

  public LocalQuickFix buildFix(PsiElement location, Object[] infos) {
    return null;
  }
}
