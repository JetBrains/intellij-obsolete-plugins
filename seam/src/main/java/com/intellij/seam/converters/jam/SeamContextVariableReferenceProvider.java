package com.intellij.seam.converters.jam;

import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.javaee.JavaeeAnnoNameReference;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.seam.utils.SeamCommonUtils;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class SeamContextVariableReferenceProvider extends PsiReferenceProvider {
  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull final PsiElement element, @NotNull final ProcessingContext context) {
    final PsiElement psiElement = CompletionUtil.getOriginalElement(element);

    if (psiElement != null) {
      final Module module = ModuleUtilCore.findModuleForPsiElement(psiElement);

      if (module != null) {
        final Object value = JavaPsiFacade.getInstance(psiElement.getProject()).getConstantEvaluationHelper()
          .computeConstantExpression(psiElement);
        if (value instanceof String) {
          final List<CommonModelElement> components = SeamCommonUtils.findSeamComponents((String)value, module);
          final CommonModelElement resolveTo = components.size() > 0 ? components.get(0) : null;

          return new PsiReference[]{new JavaeeAnnoNameReference(element, resolveTo) {
            @Override
            public Object @NotNull [] getVariants() {
              return SeamCommonUtils.getSeamContextVariableNames(module).toArray();
            }
          }};
        }
      }
    }

    return PsiReference.EMPTY_ARRAY;
  }

}
