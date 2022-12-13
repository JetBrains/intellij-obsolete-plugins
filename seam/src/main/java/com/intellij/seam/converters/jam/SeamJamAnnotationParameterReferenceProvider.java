package com.intellij.seam.converters.jam;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

public class SeamJamAnnotationParameterReferenceProvider extends PsiReferenceProvider {

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull final ProcessingContext context) {
    if (element instanceof PsiLiteralExpression) {
      final PsiLiteralExpression literalExpression = (PsiLiteralExpression)element;
      if (literalExpression.getValue() instanceof String) {

        final PsiAnnotation psiAnnotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);

        if (psiAnnotation != null) {
          return new PsiReference[]{PsiReferenceBase.createSelfReference(element, psiAnnotation)};
        }
      }
    }
    return PsiReference.EMPTY_ARRAY;
  }
}