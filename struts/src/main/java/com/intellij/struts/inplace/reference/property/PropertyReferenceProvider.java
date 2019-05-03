/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.reference.property;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.struts.inplace.InplaceUtil;
import com.intellij.struts.inplace.reference.BaseReferenceProvider;
import com.intellij.util.ProcessingContext;
import com.intellij.util.xml.DomElement;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

abstract class PropertyReferenceProvider extends BaseReferenceProvider {

  PropertyReferenceProvider(String canonicalName, Icon icon) {
    super(canonicalName);
  }

  PropertyReferenceProvider(Class<? extends DomElement> clazz) {
    super(clazz);
  }

  @Override
  @NotNull
  public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull final ProcessingContext context) {

    boolean soft = !InplaceUtil.isSimpleText(psiElement);

    PropertyReferenceSet set = new PropertyReferenceSet(this, (XmlAttributeValue)psiElement, soft);
    return set.getReferences();
  }

  /**
   * Factory method to be overriden in subclasses
   *
   * @return PropertyReference object
   */
  protected abstract PropertyReference createReference(PropertyReferenceSet set, int index, TextRange range);
}