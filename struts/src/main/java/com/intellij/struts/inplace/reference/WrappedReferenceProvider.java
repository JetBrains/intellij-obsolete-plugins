/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.reference;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * Delegates to the given ReferenceProvider if {@link #accept(PsiElement)} returns true.
 */
public class WrappedReferenceProvider extends BaseReferenceProvider {

  private final PsiReferenceProvider myProvider;

  protected WrappedReferenceProvider(final PsiReferenceProvider provider) {
    myProvider = provider;
  }

  @Override
  @NotNull
  public PsiReference[] getReferencesByElement(@NotNull final PsiElement psiElement, @NotNull final ProcessingContext context) {
    return accept(psiElement) ? myProvider.getReferencesByElement(psiElement, context) : PsiReference.EMPTY_ARRAY;
  }

  /**
   * Check if the attribute value contains a dynamic expression ("${").
   *
   * @param psiElement attribute to check
   * @return true if dynamic expression found.
   */
  protected boolean accept(@NotNull final PsiElement psiElement) {
    final String value = ((XmlAttributeValue) psiElement).getValue().trim();
    return !value.contains("${");
  }

}