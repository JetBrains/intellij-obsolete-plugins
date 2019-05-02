/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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