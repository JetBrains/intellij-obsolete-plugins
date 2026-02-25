// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.parsing.html.elements;

import com.intellij.psi.impl.source.xml.XmlDocumentImpl;
import com.intellij.psi.xml.XmlElementType;
import com.intellij.psi.xml.XmlProlog;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.plugins.grails.lang.gsp.parsing.GspElementTypes;

public class GspXmlDocument extends XmlDocumentImpl {
  public GspXmlDocument() {
    super(GspElementTypes.GSP_XML_DOCUMENT);
  }

  @Override
  public XmlProlog getProlog() {
    return (XmlProlog) findElementByTokenType(XmlElementType.XML_PROLOG);
  }

  @Override
  public XmlTag getRootTag() {
    return (XmlTag) findElementByTokenType(GspElementTypes.GSP_ROOT_TAG);
  }

  @Override
  public String toString() {
    return "PsiElement" + "(" + XmlElementType.XML_DOCUMENT.toString() + ")";
  }
}