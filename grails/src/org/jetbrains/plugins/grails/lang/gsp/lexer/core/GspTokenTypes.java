// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.lexer.core;

import com.intellij.psi.tree.CustomParsingType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.xml.XmlTokenType;
import org.jetbrains.plugins.grails.lang.gsp.lexer.IGspElementType;
import org.jetbrains.plugins.grails.lang.gsp.parsing.groovy.chameleons.GroovyDeclarationsElementType;
import org.jetbrains.plugins.grails.lang.gsp.parsing.groovy.chameleons.GroovyExpressionElementType;
import org.jetbrains.plugins.grails.lang.gsp.parsing.groovy.chameleons.GroovyMapAttributeValueType;
import org.jetbrains.plugins.grails.lang.gsp.parsing.gsp.chameleons.GspDirectiveElement;
import org.jetbrains.plugins.groovy.GroovyLanguage;

public interface GspTokenTypes {

  IElementType JSCRIPT_BEGIN = new IGspElementType("JSCRIPT_BEGIN");
  IElementType JSCRIPT_END = new IGspElementType("JSCRIPT_END");
  IElementType JEXPR_BEGIN = new IGspElementType("JEXPR_BEGIN");
  IElementType JEXPR_END = new IGspElementType("JEXPR_END");
  IElementType JDECLAR_BEGIN = new IGspElementType("JDECLAR_BEGIN");
  IElementType JDECLAR_END = new IGspElementType("JDECLAR_END");
  IElementType JDIRECT_BEGIN = new IGspElementType("JDIRECT_BEGIN");
  IElementType JDIRECT_END = new IGspElementType("JDIRECT_END");

  IElementType GEXPR_BEGIN = new IGspElementType("GEXPR_BEGIN");
  IElementType GEXPR_END = new IGspElementType("GEXPR_END");
  IElementType GSTRING_DOLLAR = new IGspElementType("GSTRING_DOLLAR");
  IElementType GSCRIPT_BEGIN = new IGspElementType("GSCRIPT_BEGIN");
  IElementType GSCRIPT_END = new IGspElementType("GSCRIPT_END");
  IElementType GDIRECT_BEGIN = new IGspElementType("GDIRECT_BEGIN");
  IElementType GDIRECT_END = new IGspElementType("GDIRECT_END");
  IElementType GDECLAR_BEGIN = new IGspElementType("GDECLAR_BEGIN");
  IElementType GDECLAR_END = new IGspElementType("GDECLAR_END");

  IElementType GSP_STYLE_COMMENT = new IGspElementType("GSP_STYLE_COMMENT");
  IElementType JSP_STYLE_COMMENT = new IGspElementType("JSP_STYLE_COMMENT");

  IElementType GROOVY_CODE = new IElementType("GROOVY_CODE", GroovyLanguage.INSTANCE);
  IElementType GROOVY_EXPR_CODE = new GroovyExpressionElementType("GROOVY_EXPR_CODE");
  IElementType GROOVY_DECLARATION = new GroovyDeclarationsElementType("GROOVY_DECLARATION");

  ///////////////////////////////////////////// Gtags & Directive lexems ///////////////////////////////////////////////

  CustomParsingType GSP_DIRECTIVE = new GspDirectiveElement("GSP_DIRECTIVE");

  IElementType GSP_WHITE_SPACE = new IGspElementType("GSP_WHITE_SPACE");
  IElementType GSP_TAG_NAME = XmlTokenType.XML_TAG_NAME;
  IElementType GSP_ATTR_NAME = new IGspElementType("GSP_ATTR_NAME");
  IElementType GSP_BAD_CHARACTER = XmlTokenType.XML_BAD_CHARACTER;
  IElementType GSP_EQ = new IGspElementType("GSP_EQ");
  IElementType GSP_ATTR_VALUE_START_DELIMITER = XmlTokenType.XML_ATTRIBUTE_VALUE_START_DELIMITER;
  IElementType GSP_ATTR_VALUE_END_DELIMITER = XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER;
  IElementType GSP_ATTRIBUTE_VALUE_TOKEN = new IGspElementType("GSP_ATTRIBUTE_VALUE_TOKEN");

  IElementType GTAG_START_TAG_START = XmlTokenType.XML_START_TAG_START;
  IElementType GTAG_TAG_END = XmlTokenType.XML_TAG_END;
  IElementType GTAG_END_TAG_START = XmlTokenType.XML_END_TAG_START;
  IElementType GTAG_START_TAG_END = XmlTokenType.XML_EMPTY_ELEMENT_END;

  IElementType GSP_MAP_ATTR_VALUE = new GroovyMapAttributeValueType("GSP_MAP_ATTR_VALUE");
  IElementType GROOVY_ATTR_VALUE = new GroovyExpressionElementType("GROOVY_ATTR_VALUE");
}
