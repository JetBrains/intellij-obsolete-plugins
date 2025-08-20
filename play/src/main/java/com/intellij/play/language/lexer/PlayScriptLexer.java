/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.play.language.lexer;

import com.intellij.lexer.Lexer;
import com.intellij.play.language.PlayFileElementTypes;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyLexer;

import static com.intellij.play.language.PlayElementTypes.*;

public class PlayScriptLexer extends PlayLexer {

  public PlayScriptLexer() {
    super(new _PlayLexer());
  }

  // %{ groovy script }%
  @Override
  protected void lookAhead(final @NotNull Lexer baseLexer) {
    while (currentOrSkipTokens(baseLexer, TokenSet.create(PERCENT))) {
      baseLexer.advance();
      if (baseLexer.getTokenType() == LEFT_BRACE) {
        addToken(baseLexer.getTokenEnd(), PlayFileElementTypes.OUTER_GROOVY_EXPRESSION_ELEMENT_TYPE);
        final int exprStart = baseLexer.getTokenEnd();
        while (currentOrSkipTokens(baseLexer, TokenSet.create(RIGHT_BRACE))) {
          int endTokenStart = baseLexer.getTokenStart();
          baseLexer.advance();
          if (baseLexer.getTokenType() == PERCENT && exprStart < endTokenStart) {
            final GroovyLexer groovyLexer = new GroovyLexer();
            groovyLexer.start(baseLexer.getBufferSequence().subSequence(exprStart, endTokenStart));
            while (groovyLexer.getTokenType() != null) {
              addToken(exprStart + groovyLexer.getTokenEnd(), groovyLexer.getTokenType());
              groovyLexer.advance();
            }
            baseLexer.advance();
            break;
          }
        }
      }
    }
    addToken(baseLexer.getTokenEnd(), PlayFileElementTypes.OUTER_GROOVY_EXPRESSION_ELEMENT_TYPE);

    advanceLexer(baseLexer);
  }
}
