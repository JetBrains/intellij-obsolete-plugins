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
