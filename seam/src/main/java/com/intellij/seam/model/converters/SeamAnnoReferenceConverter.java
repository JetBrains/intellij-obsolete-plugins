package com.intellij.seam.model.converters;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.seam.utils.SeamCommonUtils;
import com.intellij.seam.utils.beans.ContextVariable;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.CustomReferenceConverter;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

public class SeamAnnoReferenceConverter implements CustomReferenceConverter<String> {

  @Override
  public PsiReference @NotNull [] createReferences(final GenericDomValue<String> genericDomValue,
                                                   final PsiElement element,
                                                   final ConvertContext context) {
    return new PsiReference[]{new PsiReferenceBase<>(element) {

      @Override
      public PsiElement resolve() {
        final String name = genericDomValue.getStringValue();
        if (StringUtil.isEmptyOrSpaces(name)) return null;

        final Module module = context.getModule();

        for (ContextVariable contextVariable : SeamCommonUtils.getSeamContextVariablesWithDependencies(module, true, false)) {
          if (name.equals(contextVariable.getName())) {
            return contextVariable.getModelElement().getIdentifyingPsiElement();
          }
        }
        return getElement().getParent().getParent();
      }

      @Override
      public boolean isSoft() {
        return true;
      }
    }};
  }
}

