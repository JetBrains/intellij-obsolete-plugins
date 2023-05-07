package com.intellij.play.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.play.language.psi.PlayTag;
import com.intellij.play.references.PlayCustomTagNamePsiReference;
import com.intellij.play.utils.PlayBundle;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class PlayCustomTagNameInspection extends PlayBaseInspection {

  @NotNull
  @Override
  public String getShortName() {
    return "PlayCustomTagNameInspection";
  }

  @NotNull
  @Override
  public HighlightDisplayLevel getDefaultLevel() {
    return super.getDefaultLevel();
  }

  @Override
  protected void registerProblems(PsiElement element, ProblemsHolder holder) {
    if (element instanceof PlayTag) {
      for (PsiReference reference : element.getReferences()) {
        if (reference instanceof PlayCustomTagNamePsiReference) {
          if (reference.resolve() == null) {
            holder.registerProblem(reference, PlayBundle.message("PlayCustomTagNameInspection.unknown.custom.tag"),
                                   ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
          }
          return;
        }
      }
    }
  }
}
