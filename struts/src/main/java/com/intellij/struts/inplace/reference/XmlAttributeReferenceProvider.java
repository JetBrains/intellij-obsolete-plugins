/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.reference;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.ProcessingContext;
import com.intellij.util.xml.DomElement;
import org.jetbrains.annotations.NotNull;

public abstract class XmlAttributeReferenceProvider extends BaseReferenceProvider {

  protected XmlAttributeReferenceProvider(final String canonicalName) {
    super(canonicalName);
  }

  protected XmlAttributeReferenceProvider(final Class<? extends DomElement> clazz) {
    super(clazz);
  }

  @Override
  @NotNull
  public PsiReference[] getReferencesByElement(@NotNull final PsiElement psiElement,
                                               @NotNull final ProcessingContext context) {
    return getReferencesByElement(psiElement);
  }

  public PsiReference[] getReferencesByElement(@NotNull final PsiElement psiElement) {
    final String val = ((XmlAttributeValue) psiElement).getValue();
    if (val.startsWith("${") ||
        val.startsWith("<%")) {
      return PsiReference.EMPTY_ARRAY;
    }

    return create((XmlAttributeValue) psiElement);
  }

  protected abstract PsiReference[] create(final XmlAttributeValue attribute);

}
