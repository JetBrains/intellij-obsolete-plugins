// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.impl.source.xml.XmlTagImpl;
import com.intellij.psi.impl.source.xml.XmlTagValueImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTagChild;
import com.intellij.psi.xml.XmlTagValue;
import com.intellij.psi.xml.XmlText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.GspLanguage;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspXmlTagBase;

public abstract class GspXmlTagBaseImpl extends XmlTagImpl implements GspXmlTagBase {

  public GspXmlTagBaseImpl(IElementType type) {
    super(type);
  }

  @Override
  public XmlTag findParentTag() {
    return PsiTreeUtil.getParentOfType(this, XmlTag.class);
  }

  @Override
  public @NotNull XmlTagValue getValue() {
    final XmlText xmlText = PsiTreeUtil.getChildOfType(this, XmlText.class);

    if (xmlText == null) return new XmlTagValueImpl(EMPTY, this);
    return new XmlTagValueImpl(new XmlTagChild[]{xmlText}, this);
  }

  @Override
  public TreeElement addInternal(TreeElement first, ASTNode last, ASTNode anchor, Boolean before) {
    if (anchor == null &&
        getLastChildNode().getElementType().getLanguage() == GspLanguage.INSTANCE)
      return super.addInternal(first, last, getLastChildNode(), Boolean.TRUE);
    return super.addInternal(first, last, anchor, before);
  }
}
