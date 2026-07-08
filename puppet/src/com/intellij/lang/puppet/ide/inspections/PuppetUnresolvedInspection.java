package com.intellij.lang.puppet.ide.inspections;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.psi.references.PuppetNamedReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class PuppetUnresolvedInspection extends LocalInspectionTool {
  @Override
  public @Nls @NotNull String getGroupDisplayName() {
    return PuppetBundle.message("inspections.group.name");
  }

  @Override
  public boolean isEnabledByDefault() {
    return true;
  }

  @Override
  public @NotNull String getShortName() {
    return "PuppetUnresolved";
  }

  @Override
  public @NotNull PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new PsiElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        for (PsiReference reference : element.getReferences()) {
          if (reference.resolve() == null && (reference instanceof PsiPolyVariantReference)) {
            ResolveResult[] results = ((PsiPolyVariantReference)reference).multiResolve(false);
            if (results.length == 0) {
              holder.registerProblem(reference, PuppetBundle.message("inspections.unresolved.message", getRefTypeName(reference)),
                                     ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);
            }
          }
        }
      }
    };
  }

  private static String getRefTypeName(PsiReference reference) {
    if (reference instanceof PuppetNamedReference) {
      return ((PuppetNamedReference)reference).getPresentableName();
    }

    return reference.getClass().getSimpleName();
  }

}