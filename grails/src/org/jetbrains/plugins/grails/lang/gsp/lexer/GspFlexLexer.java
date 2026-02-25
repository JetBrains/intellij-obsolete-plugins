// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.lexer;

import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.MergingLexerAdapter;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.xml.XmlTokenType;
import org.jetbrains.plugins.grails.lang.gsp.lexer.core.GspTokenTypes;
import org.jetbrains.plugins.grails.lang.gsp.lexer.core._GspLexer;

public class GspFlexLexer extends MergingLexerAdapter {

  private static final TokenSet TOKENS_TO_MERGE = TokenSet.create(GspTokenTypesEx.GSP_TEMPLATE_DATA,
      XmlTokenType.XML_WHITE_SPACE,
                                                                  GspTokenTypes.GROOVY_CODE,
                                                                  GspTokenTypes.GROOVY_DECLARATION,
                                                                  GspTokenTypes.GSP_STYLE_COMMENT,
                                                                  GspTokenTypes.JSP_STYLE_COMMENT,
                                                                  GspTokenTypes.GSP_DIRECTIVE,
                                                                  GspTokenTypes.GSP_ATTRIBUTE_VALUE_TOKEN,
                                                                  GspTokenTypes.GROOVY_EXPR_CODE,
                                                                  GspTokenTypes.GSP_BAD_CHARACTER
  );

  public GspFlexLexer() {
    super(new FlexAdapter(new _GspLexer(null)), TOKENS_TO_MERGE);
  }
}
