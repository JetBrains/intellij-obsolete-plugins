// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.lexer;

import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.plugins.grails.lang.gsp.lexer.core.GspTokenTypes;
import org.jetbrains.plugins.grails.lang.gsp.parsing.html.GspHtmlTemplateRootType;
import org.jetbrains.plugins.grails.lang.gsp.parsing.outers.GspGroovyCodeElementType;
import org.jetbrains.plugins.grails.lang.gsp.parsing.outers.GspInHtmlElementType;
import org.jetbrains.plugins.grails.lang.gsp.parsing.outers.GspTemplateDataElementType;

public interface GspTokenTypesEx extends GspTokenTypes {

  // !(GspCode || GspSeparators)
  IGspElementType GSP_TEMPLATE_DATA = new GspTemplateDataElementType();

  // GSP fragment in HTML code
  IGspElementType GSP_FRAGMENT_IN_HTML = new GspInHtmlElementType();

  // Groovy fragment in GSP code
  IGspElementType GSP_GROOVY_CODE = new GspGroovyCodeElementType();

  // Html elements in GSP
  IFileElementType GSP_HTML_TEMPLATE_ROOT = new GspHtmlTemplateRootType("GspHtmlTemplateRootType");


  TokenSet GSP_COMMENTS = TokenSet.create(GSP_STYLE_COMMENT, JSP_STYLE_COMMENT);
  TokenSet GSP_GROOVY_SEPARATORS = TokenSet.create(
    JDECLAR_BEGIN,
    JEXPR_BEGIN,
    JDIRECT_BEGIN,
    JDIRECT_END,
    JSCRIPT_BEGIN,
    JSCRIPT_END,
    JEXPR_END,
    GEXPR_BEGIN,
    GEXPR_END,
    GSTRING_DOLLAR,
    GSCRIPT_BEGIN,
    GSCRIPT_END,
    GDIRECT_BEGIN,
    GDIRECT_END,
    GDECLAR_BEGIN,
    GDECLAR_END
  );
}
