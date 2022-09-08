package com.intellij.seam.el.typedHandler;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.seam.utils.SeamCommonUtils;

public class SeamElPsiClassTypedHandler extends BasicSeamElTypedHandler {

  @Override
  protected boolean isElContainerFile(final PsiFile originalFile) {

    return isSeamFacetDetected(originalFile) && originalFile instanceof PsiJavaFile;
  }

  @Override
  protected boolean isElAcceptedForElement(final PsiElement element) {
    final PsiLiteralExpression expression = PsiTreeUtil.getParentOfType(element, PsiLiteralExpression.class);
    if (expression != null) {
      PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);

      if (psiClass != null) {
          return SeamCommonUtils.isSeamClass(psiClass) || SeamCommonUtils.isAbstractSeamComponent(psiClass);
      }
    }
    return false;
  }
}
