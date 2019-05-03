/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.reference.config;

import com.intellij.psi.*;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.struts.StrutsManager;
import com.intellij.struts.TilesModel;
import com.intellij.struts.dom.tiles.Put;
import com.intellij.struts.inplace.reference.XmlAttributeReferenceProvider;
import com.intellij.struts.inplace.reference.XmlValueReference;
import com.intellij.struts.inplace.reference.XmlValueSelfReference;
import com.intellij.util.xml.DomElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author davdeev
 */
public class TilesPutsReferenceProvider extends XmlAttributeReferenceProvider {

  public TilesPutsReferenceProvider() {
    super(Put.class);

    setSoft(true);
  }

  @Override
  protected PsiReference[] create(XmlAttributeValue attribute) {

    XmlTag putTag = (XmlTag)attribute.getContext().getContext();
    final String definition = getDefinitionName(putTag);
    PsiReference ref;
    if (definition == null) {
      ref = new XmlValueSelfReference(attribute, this);
    } else {
      ref = new TilesPutsReference(attribute, definition);
    }
    return new PsiReference[] {ref};
  }

  @Nullable
  protected String getDefinitionName(XmlTag putTag) {
    XmlTag definitionTag = putTag.getParentTag();
    return definitionTag != null ? definitionTag.getAttributeValue("name") : null;
  }

  protected class TilesPutsReference extends XmlValueReference implements PsiPolyVariantReference {
    private final String definition;

    TilesPutsReference(XmlAttributeValue attribute, String definition) {

      super(attribute, TilesPutsReferenceProvider.this);
      this.definition = definition;

      mySoft = TilesPutsReferenceProvider.this.isSoft();
    }

    /**
     * @return ....
     */
    @Override
    public PsiElement doResolve() {
      TilesModel model = StrutsManager.getInstance().getTiles(myValue);
      XmlTag el = model == null ? null : model.getPutTag(definition, getValue());
      if (el != null) {
        return el;
      }
      else if (mySoft) {
        return myValue;
      }
      else {
        return null;
      }
    }

    /**
     * @return puts of the extended definitions
     */
    @Override
    @Nullable
    public Object[] doGetVariants() {
      TilesModel model = StrutsManager.getInstance().getTiles(myValue);
      return model == null ? null : getItems(model.getPuts(definition, true));
    }

    @Override
    @Nullable
    protected DomElement getScope() {
      TilesModel model = StrutsManager.getInstance().getTiles(myValue);
      if (model == null) {
        return null;
      }
      return model.findDefinition(definition);
    }

    @Override
    @NotNull
    public ResolveResult[] multiResolve(boolean b) {
      TilesModel model = StrutsManager.getInstance().getTiles(myValue);
      if (model != null) {
        Collection<Put> tags = model.getAllPuts(definition);
        if (tags != null) {
          ArrayList<ResolveResult> result = new ArrayList<>();
          String val = getValue();
          for (Put put : tags) {
            if (val.equals(put.getName().getValue())) {
              result.add(new PsiElementResolveResult(put.getName().getXmlAttributeValue()));
            }
          }
          return result.toArray(ResolveResult.EMPTY_ARRAY);
        }
      }
      return ResolveResult.EMPTY_ARRAY;
    }
  }

}
