// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.highlighter;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.xml.XmlTokenType;
import org.jetbrains.plugins.grails.lang.gsp.lexer.GspDirectiveFlexLexer;
import org.jetbrains.plugins.grails.lang.gsp.lexer.core.GspTokenTypes;

class GspDirectiveHighlightingLexer extends GspDirectiveFlexLexer {
  @Override
  public IElementType getTokenType() {
    IElementType type = super.getTokenType();
    if (type == XmlTokenType.XML_TAG_NAME) return GspTokenTypes.GSP_TAG_NAME;
    if (type == XmlTokenType.XML_NAME) return GspTokenTypes.GSP_ATTR_NAME;
    if (type == XmlTokenType.XML_WHITE_SPACE) return GspTokenTypes.GSP_WHITE_SPACE;
    if (type == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN) return GspTokenTypes.GSP_ATTRIBUTE_VALUE_TOKEN;
    if (type == XmlTokenType.XML_ATTRIBUTE_VALUE_START_DELIMITER)
      return GspTokenTypes.GSP_ATTR_VALUE_START_DELIMITER;
    if (type == XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER) return GspTokenTypes.GSP_ATTR_VALUE_END_DELIMITER;
    if (type == XmlTokenType.XML_EQ) return GspTokenTypes.GSP_EQ;
    return type;
  }
}
