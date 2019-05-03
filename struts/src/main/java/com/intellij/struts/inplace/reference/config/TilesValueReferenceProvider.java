/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.reference.config;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.WebPathReferenceProvider;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.struts.inplace.InplaceUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * @author davdeev
 */
public class TilesValueReferenceProvider extends WebPathReferenceProvider {

  private final TilesReferenceProvider tilesProvider;
  private final TilesReferenceProvider tilesProviderSoft;

  public TilesValueReferenceProvider() {
    tilesProvider = new TilesReferenceProvider(false);
    tilesProviderSoft = new TilesReferenceProvider(true);
  }

  @Override
  @NotNull
  public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull final ProcessingContext context) {
    if (element instanceof XmlAttributeValue) {
      XmlAttributeValue value = (XmlAttributeValue)element;

      if (!InplaceUtil.isSimpleText(value)) return PsiReference.EMPTY_ARRAY;
      XmlTag tag = (XmlTag)value.getContext().getContext();
      String type = tag.getAttributeValue("type");
      if ("string".equals(type)) {
        return PsiReference.EMPTY_ARRAY;
      }
      else if ("definition".equals(type)) {
        return tilesProvider.getReferencesByElement(element);
      }
      else if ("page".equals(type) ||
               "template".equals(type) ||
               (type == null && StringUtil.startsWithChar(tag.getAttributeValue("value"), '/'))) {
        String text = tag.getAttributeValue("value");
        if (InplaceUtil.containsPlaceholderReference(text)) {
          return PsiReference.EMPTY_ARRAY;
        }
        setSoft(false);
      }
      else {
        return tilesProviderSoft.getReferencesByElement(element);
      }
    }
    return super.getReferencesByElement(element);
  }
}
