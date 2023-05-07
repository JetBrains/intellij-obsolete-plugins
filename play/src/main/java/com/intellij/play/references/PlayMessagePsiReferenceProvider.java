package com.intellij.play.references;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;

public class PlayMessagePsiReferenceProvider extends PsiReferenceProvider {
  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         @NotNull ProcessingContext context) {

    Object value = null;

    if (element instanceof GrLiteral literalExpression) {
      value = literalExpression.getValue();
    }

    if (value instanceof String && !((String)value).contains("\n")) {
      return new PsiReference[]{new PlayPropertyReference((String)value, element)};
    }
    return PsiReference.EMPTY_ARRAY;
  }
}
