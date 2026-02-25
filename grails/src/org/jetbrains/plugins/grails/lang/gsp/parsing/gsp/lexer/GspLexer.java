// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.parsing.gsp.lexer;

import com.intellij.lexer.DelegateLexer;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.plugins.grails.lang.gsp.lexer.GspFlexLexer;
import org.jetbrains.plugins.grails.lang.gsp.lexer.core.GspTokenTypes;

import static org.jetbrains.plugins.grails.lang.gsp.lexer.GspTokenTypesEx.GSP_GROOVY_CODE;

public class GspLexer extends DelegateLexer implements GspTokenTypes {
  public GspLexer() {
    super(new GspFlexLexer());
  }

  @Override
  public IElementType getTokenType() {
    return convertToken(super.getTokenType());
  }

  /**
   * Converts token for GSP representation
   */
  private static IElementType convertToken(IElementType tokenType) {
    if (GROOVY_EXPR_CODE == tokenType ||
        GSP_MAP_ATTR_VALUE == tokenType ||
        GROOVY_ATTR_VALUE == tokenType ||
        GROOVY_CODE == tokenType ||
        GROOVY_DECLARATION == tokenType) {
      return GSP_GROOVY_CODE;
    }
    return tokenType;
  }
}
