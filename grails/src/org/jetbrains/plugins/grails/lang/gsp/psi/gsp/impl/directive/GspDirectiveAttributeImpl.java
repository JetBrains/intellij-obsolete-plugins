// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.directive;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.xml.XmlAttributeImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.xml.XmlAttributeValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.parsing.GspElementTypes;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.directive.GspDirectiveAttribute;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.directive.GspDirectiveAttributeValue;

public class GspDirectiveAttributeImpl extends XmlAttributeImpl implements GspDirectiveAttribute {
  public GspDirectiveAttributeImpl() {
    super();
  }

  @Override
  public @NotNull IElementType getElementType() {
    return GspElementTypes.GSP_DIRECTIVE_ATTRIBUTE;
  }

  @Override
  public XmlAttributeValue getValueElement() {
    ASTNode node = findChildByType(GspElementTypes.GSP_DIRECTIVE_ATTRIBUTE_VALUE);
    if (node != null) {
      PsiElement psi = node.getPsi();
      assert psi instanceof GspDirectiveAttributeValue;
      return (GspDirectiveAttributeValue) psi;
    }
    return null;
  }

  @Override
  public String toString() {
    return "GSP_DIRECTIVE_ATTRIBUTE";
  }

}
