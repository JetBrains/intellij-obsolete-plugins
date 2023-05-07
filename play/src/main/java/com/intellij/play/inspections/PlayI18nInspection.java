package com.intellij.play.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.play.references.PlayPropertyReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;

//import com.intellij.play.references.PlayPropertyReference;

final class PlayI18nInspection extends PlayBaseInspection {
  @NotNull
  @Override
  public String getShortName() {
    return "PlayPropertyInspection";
  }

  @NotNull
  @Override
  public HighlightDisplayLevel getDefaultLevel() {
    return super.getDefaultLevel();
  }

  @Override
  protected void registerProblems(PsiElement element, ProblemsHolder holder) {
    if (element instanceof GrLiteral) {
      for (PsiReference reference : element.getReferences()) {
        if (reference instanceof PlayPropertyReference) {
          if (((PlayPropertyReference)reference).multiResolve(false).length == 0) {
            holder.registerProblem(reference, ((PlayPropertyReference)reference).getUnresolvedMessagePattern(),
                                   ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
          }
        }
      }
    }
  }
}
