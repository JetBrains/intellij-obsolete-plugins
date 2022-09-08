package com.intellij.seam.model.references;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.CustomReferenceConverter;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

public class SeamObserverEventTypeReferenceConverter implements CustomReferenceConverter<String> {

  @Override
  public PsiReference @NotNull [] createReferences(final GenericDomValue<String> genericDomValue,
                                                   final PsiElement element,
                                                   final ConvertContext context) {
    return new PsiReference[]{new SeamObserverEventTypeReference<>(element) {
      @Override
      protected String getEventType(final PsiElement psiElement) {
        return genericDomValue.getStringValue();
      }
    }};
  }
}


