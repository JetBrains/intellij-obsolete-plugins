package com.intellij.lang.puppet.ide.inspections;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.psi.PsiPuppetVarWrapper;
import com.intellij.lang.puppet.psi.PsiPuppetVisitor;
import com.intellij.lang.puppet.util.PuppetConfigurationUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class Puppet4DeprecationsInspection extends LocalInspectionTool {
  @Override
  public @NotNull PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new PsiPuppetVisitor() {
      @Override
      public void visitVarWrapper(@NotNull PsiPuppetVarWrapper var) {
        if (PuppetConfigurationUtil.getPuppetVersion(var) != PuppetLanguage.Version.PUPPET_4) {
          return;
        }

        String variableName = var.getName();
        if (StringUtil.isEmpty(variableName)) {
          return;
        }

        if (StringUtil.isCapitalized(variableName)) {
          holder.registerProblem(var, PuppetBundle.message("inspections.deprecated.in.puppet4.capitalized.variables"),
                                 ProblemHighlightType.LIKE_DEPRECATED);
        }
        else if (variableName.startsWith("_") && var.isFullQualified()) {
          holder.registerProblem(var, PuppetBundle.message("inspections.deprecated.in.puppet4.underscore.in.variables"),
                                 ProblemHighlightType.LIKE_DEPRECATED);
        }
      }
    };
  }
}
