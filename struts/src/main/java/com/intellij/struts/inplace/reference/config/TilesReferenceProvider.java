/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.reference.config;

import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.struts.StrutsManager;
import com.intellij.struts.TilesModel;
import com.intellij.struts.dom.tiles.Definition;
import com.intellij.struts.inplace.reference.XmlAttributeReferenceProvider;
import com.intellij.struts.inplace.reference.XmlValueReference;
import com.intellij.util.xml.DomElement;
import org.jetbrains.annotations.Nullable;

/**
 * Reference to tiles definition.
 */
public class TilesReferenceProvider extends XmlAttributeReferenceProvider {

  public TilesReferenceProvider(boolean soft) {
    super(Definition.class);
    setSoft(soft);
  }

  @Override
  protected PsiReference[] create(final XmlAttributeValue attribute) {

    final PsiReference ref = new XmlValueReference(attribute, this) {

      @Override
      @Nullable
      public XmlTag doResolve() {
        final TilesModel model = StrutsManager.getInstance().getTiles(myValue);
        final String tileName = getValue();
        return model == null ? null : model.getTileTag(tileName);
      }

      @Override
      @Nullable
      protected DomElement getScope() {
        final TilesModel model = StrutsManager.getInstance().getTiles(myValue);
        return model == null ? null : model.getMergedModel();
      }

      @Override
      @Nullable
      public Object[] doGetVariants() {
        final TilesModel model = StrutsManager.getInstance().getTiles(myValue);
        return model == null ? null : getItems(model.getDefinitions());
      }

    };

    return new PsiReference[]{ref};
  }

}