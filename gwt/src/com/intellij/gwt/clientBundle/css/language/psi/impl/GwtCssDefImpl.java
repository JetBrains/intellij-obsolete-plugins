package com.intellij.gwt.clientBundle.css.language.psi.impl;

import com.intellij.gwt.clientBundle.css.language.GwtCssElementTypes;
import com.intellij.gwt.clientBundle.css.language.psi.GwtCssDef;

public class GwtCssDefImpl extends GwtCssDeclarationElementBase implements GwtCssDef {
  public GwtCssDefImpl() {
    super(GwtCssElementTypes.CSS_DEF);
  }
}
