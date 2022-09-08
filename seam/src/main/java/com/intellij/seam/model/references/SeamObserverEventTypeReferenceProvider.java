package com.intellij.seam.model.references;

import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReference;

public class SeamObserverEventTypeReferenceProvider extends BasePsiLiteralExpressionReferenceProvider {

  @Override
  protected PsiReference getPsiLiteralExpressionReference(final PsiLiteralExpression literalExpression) {
    return new SeamObserverEventTypeReference<>(literalExpression) {
      @Override
      protected String getEventType(final PsiLiteralExpression psiElement) {
        return (String)psiElement.getValue();
      }
    };
  }
}
