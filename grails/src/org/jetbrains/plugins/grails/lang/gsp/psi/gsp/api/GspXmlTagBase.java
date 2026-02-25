// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api;

import com.intellij.psi.impl.source.jsp.jspXml.JspXmlTagBase;
import com.intellij.psi.xml.XmlTag;

public interface GspXmlTagBase extends GspTag, JspXmlTagBase {

  @Override
  XmlTag findParentTag();

}
