// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.common;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.util.GrailsPsiUtil;

import java.util.ArrayList;
import java.util.List;

public class XmlGspTagWrapper implements GspTagWrapper {

  private final XmlTag myTag;

  public XmlGspTagWrapper(XmlTag tag) {
    myTag = tag;
  }

  @Override
  public @NotNull String getTagName() {
    return myTag.getName();
  }

  @Override
  public boolean hasAttribute(@NotNull String name) {
    return myTag.getAttribute(name) != null;
  }

  @Override
  public XmlAttributeValue getAttributeValue(@NotNull String name) {
    XmlAttribute attrValue = myTag.getAttribute(name);
    if (attrValue == null) return null;
    return attrValue.getValueElement();
  }

  @Override
  public PsiType getAttributeValueType(@NotNull String name) {
    XmlAttributeValue attribute = getAttributeValue(name);
    if (attribute == null) return null;
    return GrailsPsiUtil.getAttributeExpressionType(attribute);
  }

  @Override
  public String getAttributeText(@NotNull PsiElement element) {
    XmlAttributeValue attributeValue = (XmlAttributeValue)element;
    if (!GrailsPsiUtil.isSimpleAttribute(attributeValue)) return null;
    return attributeValue.getValue();
  }

  @Override
  public List<String> getAttributeNames() {
    XmlAttribute[] attributes = myTag.getAttributes();
    List<String> res = new ArrayList<>(attributes.length);
    
    for (XmlAttribute attribute : attributes) {
      res.add(attribute.getName());
    }
    
    return res;
  }
}
