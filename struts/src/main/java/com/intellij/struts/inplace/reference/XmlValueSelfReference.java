/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.reference;

import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttributeValue;

/**
 * @author davdeev
 */
public class XmlValueSelfReference extends XmlValueReference {

  public XmlValueSelfReference(XmlAttributeValue attribute, BaseReferenceProvider provider) {
    super(attribute, provider);
  }

  @Override
  protected PsiElement doResolve() {
    return myValue;
  }

  @Override
  protected Object[] doGetVariants() {
    return EMPTY_ARRAY;
  }

}
