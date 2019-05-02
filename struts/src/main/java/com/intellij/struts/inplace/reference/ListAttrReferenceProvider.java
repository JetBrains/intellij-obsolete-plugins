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

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.ProcessingContext;
import com.intellij.util.xml.DomElement;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;

public abstract class ListAttrReferenceProvider extends BaseReferenceProvider {

  private final boolean single;

  protected ListAttrReferenceProvider(String canonicalName, Icon icon, boolean single) {
    super(canonicalName);
    this.single = single;
  }

  protected ListAttrReferenceProvider(Class<? extends DomElement> clazz, boolean single) {
    super(clazz);
    this.single = single;
  }

  @Override
  @NotNull
  public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull final ProcessingContext context) {

    if (single) {
      return new PsiReference[]{create((XmlAttributeValue)psiElement, null)};
    }
    String s = ((XmlAttributeValue)psiElement).getValue();

    int offset = 1;
    ArrayList<PsiReference> arraylist = new ArrayList<>();
    int i = -1;
    int k;
    do {
      k = s.indexOf(',', i + 1);
      int start = offset + i + 1;
      int end = offset + (k <= 0 ? s.length() : k);

      while (start < s.length() - 1 && Character.isWhitespace(s.charAt(start - 1))) {
        start++;
      }
      while (end > 2 && Character.isWhitespace(s.charAt(end - 2))) {
        end--;
      }
      TextRange range = new TextRange(start, end);
      arraylist.add(create((XmlAttributeValue)psiElement, range));
    }
    while ((i = k) >= 0);
    return arraylist.toArray(PsiReference.EMPTY_ARRAY);
  }

  @NotNull
  abstract protected XmlValueReference create(XmlAttributeValue attribute, TextRange range);

}
