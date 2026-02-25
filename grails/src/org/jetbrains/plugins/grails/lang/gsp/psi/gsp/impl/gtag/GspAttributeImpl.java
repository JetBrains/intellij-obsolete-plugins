// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.gtag;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.xml.XmlAttributeDelegate;
import com.intellij.psi.impl.source.xml.XmlAttributeImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.xml.XmlAttributeValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.parsing.GspElementTypes;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspAttribute;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspAttributeValue;

public class GspAttributeImpl extends XmlAttributeImpl implements GspAttribute {

  @Override
  public String toString() {
    return "GSP attribute";
  }

  @Override
  public @NotNull IElementType getElementType() {
    return GspElementTypes.GRAILS_TAG_ATTRIBUTE;
  }

  @Override
  public XmlAttributeValue getValueElement() {
    ASTNode node = findChildByType(GspElementTypes.GSP_ATTRIBUTE_VALUE);
    if (node != null) {
      PsiElement psi = node.getPsi();
      assert psi instanceof GspAttributeValue;
      return (GspAttributeValue)psi;
    }
    return null;
  }

  @Override
  protected @NotNull XmlAttributeDelegate createDelegate() {
    return new GspAttributeImplDelegate();
  }

  private class GspAttributeImplDelegate extends XmlAttributeImplDelegate {
    @Override
    protected void appendChildToDisplayValue(@NotNull StringBuilder buffer, @NotNull ASTNode child) {
      //super.appendChildToDisplayValue(buffer, child);
      CharSequence sq = child.getChars();
      boolean slash = false;
      for (int i = 0; i < sq.length(); i++) {
        char a = sq.charAt(i);

        if (slash) {
          buffer.append(a);
          slash = false;
        }
        else {
          if (a == '\\') {
            slash = true;
          }
          else {
            buffer.append(a);
          }
        }
      }
    }
  }
}
