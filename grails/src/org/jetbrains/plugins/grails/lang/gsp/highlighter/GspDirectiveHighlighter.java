// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.grails.lang.gsp.highlighter;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.JspHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.lexer.core.GspTokenTypes;

public class GspDirectiveHighlighter extends SyntaxHighlighterBase implements GspTokenTypes {
  static final TokenSet tGSP_DIRECT_SEPARATORS = TokenSet.create(
      GTAG_END_TAG_START,
      GTAG_START_TAG_START,
      GTAG_START_TAG_END,
      GTAG_TAG_END,
      JDIRECT_BEGIN,
      JDIRECT_END,
      GDIRECT_BEGIN,
      GDIRECT_END);

  static final TokenSet tGSP_DIRECT_TOKENS = TokenSet.create(
      GSP_WHITE_SPACE,
      GSP_TAG_NAME,
      GSP_ATTR_NAME,
      GSP_EQ,
      GSP_BAD_CHARACTER,
      GSP_ATTR_VALUE_START_DELIMITER,
      GSP_ATTR_VALUE_END_DELIMITER,
      GSP_ATTRIBUTE_VALUE_TOKEN
  );

  @Override
  public @NotNull Lexer getHighlightingLexer() {
    return new GspDirectiveHighlightingLexer();
  }

  @Override
  public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
    if (tGSP_DIRECT_SEPARATORS.contains(tokenType)) {
      return pack(JspHighlighterColors.JSP_DIRECTIVE_STAT_END_MARKER);
    }
    if (tokenType == GSP_ATTR_NAME) {
      return pack(JspHighlighterColors.JSP_ATTRIBUTE_NAME);
    }
    if (tokenType == GSP_TAG_NAME) {
      return pack(JspHighlighterColors.JSP_ACTION_AND_DIRECTIVE_NAME);
    }
    if (tokenType == GSP_ATTRIBUTE_VALUE_TOKEN) {
      return pack(JspHighlighterColors.JSP_ATTRIBUTE_VALUE);
    }
    if (tGSP_DIRECT_TOKENS.contains(tokenType)) {
      return pack(JspHighlighterColors.JSP_ACTION_AND_DIRECTIVE_BACKGROUND);
    }
    return TextAttributesKey.EMPTY_ARRAY;
  }
}