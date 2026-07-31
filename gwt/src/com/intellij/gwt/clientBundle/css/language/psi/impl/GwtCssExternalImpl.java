package com.intellij.gwt.clientBundle.css.language.psi.impl;

import com.intellij.gwt.clientBundle.css.language.GwtCssElementTypes;
import com.intellij.gwt.clientBundle.css.language.psi.GwtCssExternal;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.css.impl.CssElementTypes;
import com.intellij.psi.impl.source.tree.CompositePsiElement;

import java.util.ArrayList;
import java.util.List;

public class GwtCssExternalImpl extends CompositePsiElement implements GwtCssExternal {
  public GwtCssExternalImpl() {
    super(GwtCssElementTypes.CSS_EXTERNAL);
  }

  @Override
  public List<String> getSelectorNames() {
    final List<String> result = new ArrayList<>();
    for (ASTNode child = getFirstChildNode(); child != null; child = child.getTreeNext()) {
      if (child.getElementType() == CssElementTypes.CSS_IDENT) {
        final String name = child.getText();
        if (!StringUtil.isEmpty(name)) {
          result.add(name);
        }
      }
    }
    return result;
  }
}
