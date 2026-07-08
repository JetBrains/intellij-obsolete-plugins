package com.intellij.lang.puppet.ide.inspections;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.psi.PsiPuppetImportStatement;
import com.intellij.lang.puppet.psi.PsiPuppetVisitor;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class Puppet3DeprecationsInspection extends LocalInspectionTool {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new PsiPuppetVisitor() {
      @Override
      public void visitImportStatement(@NotNull PsiPuppetImportStatement o) {
        holder.registerProblem(o, PuppetBundle.message("inspections.deprecated.in.puppet3.import"), ProblemHighlightType.LIKE_DEPRECATED);
        super.visitImportStatement(o);
      }
    };
  }
}
