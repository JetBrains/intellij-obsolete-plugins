package com.intellij.seam.model.references;

import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReference;

public class SeamRaiseEventRefernceProvider extends BasePsiLiteralExpressionReferenceProvider {

  @Override
  protected PsiReference getPsiLiteralExpressionReference(final PsiLiteralExpression literalExpression) {
    return new SeamEventTypeReference.SeamLiteralExpression(literalExpression);
  }
}