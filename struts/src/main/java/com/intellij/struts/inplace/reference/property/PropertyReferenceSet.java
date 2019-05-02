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

package com.intellij.struts.inplace.reference.property;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;

import java.util.ArrayList;

/**
 * @author davdeev
 */
class PropertyReferenceSet {

  private final PropertyReference[] references;
  private final XmlAttributeValue value;

  PropertyReferenceSet(PropertyReferenceProvider provider, XmlAttributeValue value, boolean soft) {

    this.value = value;

    String s = value.getValue();

    int offset = 1;
    ArrayList<PropertyReference> arraylist = new ArrayList<>();
    int i = -1;
    int pos;
    int index = 0;

    PsiElement tag = value.getContext().getContext();
    if (tag instanceof XmlTag) {
      String name = ((XmlTag)tag).getAttributeValue("name");
      if (name == null) {
        name = ((XmlTag)tag).getAttributeValue("collection");
      }
      if (name != null) {
        references = new PropertyReference[0];
        return;
      }
    }

    do {
      pos = s.indexOf('.', i + 1);
      TextRange range = new TextRange(offset + i + 1, offset + (pos <= 0 ? s.length() : pos));
      PropertyReference ref = provider.createReference(this, index, range);

      if (index == 0 && !soft && ref instanceof FormPropertyReference) {
        boolean[] softRef = new boolean[1];
        ((FormPropertyReference)ref).getFormBean(softRef);
        soft = softRef[0];
      }

      ref.setSoft(soft);
      arraylist.add(ref);
      index++;
    }
    while ((i = pos) >= 0);

    references = arraylist.toArray(new PropertyReference[0]);
  }

  public PropertyReference[] getReferences() {
    return references;
  }

  public XmlAttributeValue getValue() {
    return value;
  }
}
