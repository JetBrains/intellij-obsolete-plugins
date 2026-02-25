// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.grails.lang.gsp.highlighter;

import com.intellij.javaee.el.impl.ELHighlighter;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.JspHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.lexer.GspFlexLexer;
import org.jetbrains.plugins.grails.lang.gsp.lexer.GspTokenTypesEx;
import org.jetbrains.plugins.groovy.highlighter.GroovySyntaxHighlighter;

public final class GspSyntaxHighlighter extends SyntaxHighlighterBase implements GspTokenTypesEx {
  private final GspDirectiveHighlighter myDirectiveHighlighter = new GspDirectiveHighlighter();

  @Override
  public @NotNull Lexer getHighlightingLexer() {
    return new GspFlexLexer();
  }

  static final TokenSet tGSP_SEPARATORS_NOT_DIRECT = TokenSet.create(
          JSCRIPT_BEGIN,
          JDECLAR_BEGIN,
          JDECLAR_END,
          JEXPR_BEGIN,
          JSCRIPT_END,
          JEXPR_END,
          GEXPR_BEGIN,
          GEXPR_END,
          GSTRING_DOLLAR,
          GSCRIPT_BEGIN,
          GSCRIPT_END,
          GDECLAR_BEGIN,
          GDECLAR_END);

  @Override
  public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
    if (tGSP_SEPARATORS_NOT_DIRECT.contains(tokenType)) {
      return pack(ELHighlighter.EL_BOUNDS);
    }
    if (GspTokenTypesEx.GSP_COMMENTS.contains(tokenType)) {
      return pack(GroovySyntaxHighlighter.BLOCK_COMMENT);
    }
    if (myDirectiveHighlighter.getTokenHighlights(tokenType).length > 0) {
      return pack(JspHighlighterColors.JSP_ACTION_AND_DIRECTIVE_BACKGROUND, myDirectiveHighlighter.getTokenHighlights(tokenType));
    }
    return TextAttributesKey.EMPTY_ARRAY;
  }
}
