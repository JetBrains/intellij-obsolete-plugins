/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
