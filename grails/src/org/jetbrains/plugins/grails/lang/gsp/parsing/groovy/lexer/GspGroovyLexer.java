// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.parsing.groovy.lexer;

import com.intellij.lexer.Lexer;
import com.intellij.lexer.LexerPosition;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.xml.XmlTokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.lexer.GspFlexLexer;
import org.jetbrains.plugins.grails.lang.gsp.lexer.GspTokenTypesEx;
import org.jetbrains.plugins.grails.lang.gsp.lexer.core.GspTokenTypes;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyLexer;

public class GspGroovyLexer extends Lexer implements GspTokenTypes {

  static final TokenSet TOKENS_TO_IGNORE = TokenSet.create(
          GTAG_END_TAG_START,
          GTAG_START_TAG_END,
          GTAG_START_TAG_START,
          GTAG_TAG_END,
          JDIRECT_BEGIN,
          JDIRECT_END,
          GDIRECT_BEGIN,
          GDIRECT_END,
          GSP_WHITE_SPACE,
          GSP_TAG_NAME,
          GSP_ATTR_NAME,
          GSP_EQ,
          GSP_BAD_CHARACTER,
          GSP_ATTR_VALUE_START_DELIMITER,
          GSP_ATTR_VALUE_END_DELIMITER,
          GSP_ATTRIBUTE_VALUE_TOKEN,
          GSP_STYLE_COMMENT,
          JSP_STYLE_COMMENT
  );


  private final Lexer myGspLexer = new GspFlexLexer();
  private final GroovyLexer myGroovyLexer = new GroovyLexer();

  private Lexer myCurrentGroovyLexer = null;

  @Override
  public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
    myGspLexer.start(buffer, startOffset, endOffset, initialState);
    setUpGroovyLexer();
  }

  @Override
  public int getState() {
    return myGspLexer.getState();
  }

  @Override
  public @Nullable IElementType getTokenType() {
    IElementType tokenType = myGspLexer.getTokenType();

    if (GROOVY_CODE.equals(tokenType)) {
      return myCurrentGroovyLexer.getTokenType();
    }
    return convertTokenType(tokenType);
  }

  private static IElementType convertTokenType(IElementType tokenType) {
    if (TOKENS_TO_IGNORE.contains(tokenType) ||
        GSP_DIRECTIVE == tokenType ||
        XmlTokenType.XML_WHITE_SPACE == tokenType) {
      return GspTokenTypesEx.GSP_TEMPLATE_DATA;
    }
    return tokenType;
  }

  @Override
  public int getTokenStart() {
    IElementType tokenType = myGspLexer.getTokenType();
    if (tokenType == GROOVY_CODE) {
      return myCurrentGroovyLexer.getTokenStart();
    }
    return myGspLexer.getTokenStart();
  }

  @Override
  public int getTokenEnd() {
    IElementType tokenType = myGspLexer.getTokenType();
    if (tokenType == GROOVY_CODE) {
      return myCurrentGroovyLexer.getTokenEnd();
    }
    return myGspLexer.getTokenEnd();
  }

  @Override
  public void advance() {
    IElementType tokenType = myGspLexer.getTokenType();
    if (tokenType == GROOVY_CODE) {
      myCurrentGroovyLexer.advance();
      if (myCurrentGroovyLexer.getTokenType() != null) {
        return;
      }
    }
    myGspLexer.advance();
    setUpGroovyLexer();
  }

  private void setUpGroovyLexer() {
    while (true) {
      IElementType tokenType = myGspLexer.getTokenType();
      if (tokenType == GROOVY_CODE) {
        myCurrentGroovyLexer = myGroovyLexer;
      } else {
        return;
      }

      myCurrentGroovyLexer.start(myGspLexer.getBufferSequence(), myGspLexer.getTokenStart(), myGspLexer.getTokenEnd());
      if (myCurrentGroovyLexer.getTokenType() != null) {
        return;
      }
      myGspLexer.advance();
    }
  }


  private static class GspPosition implements LexerPosition {
    private final LexerPosition myGroovyPosition;
    private final LexerPosition myGspPosition;

    GspPosition(final LexerPosition groovyPosition, final LexerPosition gspPosition) {
      myGroovyPosition = groovyPosition;
      myGspPosition = gspPosition;
    }

    @Override
    public int getOffset() {
      final int gspPos = myGspPosition != null ? myGspPosition.getOffset() : 0;
      final int groovyPos = myGroovyPosition == null ? 0 : myGroovyPosition.getOffset();
      return Math.max(gspPos, groovyPos);
    }

    public LexerPosition getGroovyPosition() {
      return myGroovyPosition;
    }

    public LexerPosition getGspPosition() {
      return myGspPosition;
    }

    @Override
    public int getState() {
      throw new UnsupportedOperationException("Method getState is not yet implemented in " + getClass().getName());
    }
  }

  @Override
  public @NotNull LexerPosition getCurrentPosition() {
    return new GspPosition(myCurrentGroovyLexer != null ? myCurrentGroovyLexer.getCurrentPosition() : null,
            myGspLexer.getCurrentPosition());
  }

  @Override
  public void restore(@NotNull LexerPosition position) {
    if (position instanceof GspPosition gspPosition) {
      myGspLexer.restore(gspPosition);

      LexerPosition groovyPosition = gspPosition.getGroovyPosition();
      if (groovyPosition != null &&
              myCurrentGroovyLexer != null &&
              groovyPosition.getOffset() < myCurrentGroovyLexer.getBufferEnd()) {
        myCurrentGroovyLexer.restore(groovyPosition);
      } else {
        myCurrentGroovyLexer = null;
        setUpGroovyLexer();
      }
    }

  }

  @Override
  public @NotNull CharSequence getBufferSequence() {
    return myGspLexer.getBufferSequence();
  }

  @Override
  public int getBufferEnd() {
    return myGspLexer.getBufferEnd();
  }
}
