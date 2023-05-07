/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.play.language.lexer;

import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.Lexer;
import com.intellij.lexer.LookAheadLexer;
import com.intellij.play.language.PlayTokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.play.language.PlayElementTypes.*;

public class PlayLexer extends LookAheadLexer {

  public static final TokenSet START_MARKUP_TOKENS =
    TokenSet.create(EL_START,
                    ACTION_START,
                    ACTION_END,
                    MESSAGE_START,
                    ACTION_DOUBLE_START,
                    TAG_START,
                    END_TAG_START,
                    PERCENT,
                    ASTERISK);

  private final TokenSet BRACKETS_SET = TokenSet.create(LEFT_PAREN, RIGHT_PAREN, LEFT_BRACKET, RIGHT_BRACKET, LEFT_BRACE);

  protected PlayLexer(_PlayLexer rawLexer) {
    super(new FlexAdapter(rawLexer));
  }

  @Nullable
  protected IElementType mergeTokens(Lexer baseLexer, TokenSet until, IElementType intoType) {
    if (skipTokens(baseLexer, until)) {
      addToken(baseLexer.getTokenStart(), intoType);
    }
    return baseLexer.getTokenType();
  }

  protected static boolean currentOrSkipTokens(Lexer baseLexer, TokenSet until) {
    return until.contains(baseLexer.getTokenType()) || skipTokens(baseLexer, until);
  }

  protected static boolean skipTokens(Lexer baseLexer, TokenSet until) {
    boolean skipped = false;
    while (true) {
      IElementType tokenType = baseLexer.getTokenType();
      if (tokenType == null || until.contains(tokenType)) {
        return skipped;
      }
      skipped = true;
      baseLexer.advance();
    }
  }

  @Override
  protected void lookAhead(final @NotNull Lexer baseLexer) {
    mergeTokens(baseLexer, START_MARKUP_TOKENS, TEMPLATE_TEXT);

    IElementType tokenType = baseLexer.getTokenType();

    if (tokenType == ACTION_START || tokenType == ACTION_DOUBLE_START) {
      tokenizeAction(baseLexer);
    }
    else if (tokenType == MESSAGE_START) {
      tokenizeMessage(baseLexer);
    }
    else if (tokenType == EL_START) {
      tokenizeExpression(baseLexer);
    }
    else if (tokenType == TAG_START || tokenType == END_TAG_START) {
      tokenizeTag(baseLexer);
    }
    else if (tokenType == ASTERISK) {
      baseLexer.advance();
      if (baseLexer.getTokenType() == LEFT_BRACE) {
        advanceAs(baseLexer, COMMENT_START);
        tokenize(baseLexer, COMMENT_TEXT, COMMENT_END, ASTERISK, false);
      }
      else {
        addToken(baseLexer.getTokenStart(), TEMPLATE_TEXT);
      }
    } else {
      super.lookAhead(baseLexer);
    }
  }

  private void tokenizeScript(Lexer baseLexer) {
    tokenize(baseLexer, GROOVY_SCRIPT, SCRIPT_END, PERCENT, false);
  }

  private void tokenize(Lexer baseLexer, PlayTokenType bodyType, PlayTokenType endType, IElementType endSymbol, boolean addScriptNode) {
    TokenSet set = TokenSet.create(RIGHT_BRACE);
    if (baseLexer.getTokenType() == RIGHT_BRACE) {
      int tokenStart = baseLexer.getTokenStart();
      baseLexer.advance();
      if (baseLexer.getTokenType() == endSymbol) {
        if (addScriptNode) {
          addToken(tokenStart, bodyType);
        }
        advanceWithReplace(baseLexer, endType);
      }
    }
    else if (skipTokens(baseLexer, set)) {
      int tokenStart = baseLexer.getTokenStart();
      baseLexer.advance();
      if (baseLexer.getTokenType() == endSymbol) {
        addToken(tokenStart, bodyType);
        advanceWithReplace(baseLexer, endType);
      }
      else {
        tokenize(baseLexer, bodyType, endType, endSymbol, true);
      }
    }
    else {
      advanceAs(baseLexer, bodyType);
    }
  }

  private void tokenizeExpression(Lexer baseLexer) {
    advanceLexer(baseLexer);
    mergeToRightBrace(baseLexer, EL_EXPRESSION, EL_END);
  }

  private void tokenizeTag(Lexer baseLexer) {
    boolean isStartTag = baseLexer.getTokenType() == TAG_START;
    advanceLexer(baseLexer);
    mergeTokens(baseLexer, TokenSet.create(RIGHT_BRACE, WHITE_SPACE, CLOSE_TAG), TAG_NAME);

    addWhiteSpaces(baseLexer);
    if (isStartTag) {
      tokenizeTagExpression(baseLexer);
    }
    else {
      mergeToTagEnd(baseLexer);
    }
  }

  private void tokenizeTagExpression(Lexer baseLexer) {
    tokenizeTagExpression(baseLexer, TokenSet.create(TAG_START, RIGHT_BRACE, CLOSE_TAG, COMMA, COLON, SINGLE_QUOTE, DOUBLE_QUOTE));
  }

  private boolean tokenizeTagExpression(Lexer baseLexer, TokenSet until) {
    addWhiteSpaces(baseLexer);
    if (baseLexer.getTokenType() == RIGHT_BRACE || baseLexer.getTokenType() == CLOSE_TAG) {
      advanceTagEnd(baseLexer);
    }
    else if (baseLexer.getTokenType() == AT) {
      advanceLexer(baseLexer);
      tokenizeExpression(baseLexer, until, ACTION_SCRIPT);
    }
    else if (baseLexer.getTokenType() == TAG_START || baseLexer.getTokenType() == END_TAG_START) {
      tokenizeTag(baseLexer);
    }
    else {
      tokenizeExpression(baseLexer, until, TAG_EXPRESSION);
    }
    return true;
  }

  private boolean tokenizeExpression(Lexer baseLexer, TokenSet until, IElementType type) {
    return tokenizeExpression(baseLexer, until, type, new CounterHelper(baseLexer.getTokenEnd()));
  }

  private boolean tokenizeExpression(Lexer baseLexer, TokenSet until, IElementType type, @NotNull CounterHelper counter) {
    IElementType tokenType = baseLexer.getTokenType();
    if (tokenType == SINGLE_QUOTE || tokenType == DOUBLE_QUOTE) {
      return tokenizeExpressionWithQuotes(baseLexer, until, type, counter, tokenType);
    }

    if (counter.hasNestingBrackets()) {
      if (TokenSet.orSet(BRACKETS_SET, TokenSet.create(RIGHT_BRACE)).contains(baseLexer.getTokenType())) {
        countBrackets(baseLexer, counter);
        baseLexer.advance();
        if (baseLexer.getTokenType() == null) {
          addToken(baseLexer.getTokenStart(), type);
        }
        else {
          return tokenizeExpression(baseLexer, until, type, counter);
        }
      }
      else if (skipTokens(baseLexer, TokenSet.orSet(BRACKETS_SET, TokenSet.create(CLOSE_TAG)))) {
        if (baseLexer.getTokenType() == CLOSE_TAG) {
          // incorrect braces: '/>' or '}'
          addToken(baseLexer.getTokenStart(), type);
          return completeTokenizeExpression(baseLexer);
        }
        else {
          countBrackets(baseLexer, counter);
          baseLexer.advance();
          return tokenizeExpression(baseLexer, until, type, counter);
        }
      }
      else {
        addToken(baseLexer.getTokenStart(), type);
        return completeTokenizeExpression(baseLexer); // ?
      }
    }


    final TokenSet startBracketSet = TokenSet.create(LEFT_PAREN, LEFT_BRACKET, LEFT_BRACE);
    if (currentOrSkipTokens(baseLexer, TokenSet.orSet(until, startBracketSet))) {
      if (BRACKETS_SET.contains(baseLexer.getTokenType())) {
        countBrackets(baseLexer, counter);
        baseLexer.advance();
        return tokenizeExpression(baseLexer, until, type, counter);
      }

      IElementType elementType = baseLexer.getTokenType();
      if (elementType == SINGLE_QUOTE || elementType == DOUBLE_QUOTE) {
        return tokenizeExpressionWithQuotes(baseLexer, until, type, counter, elementType);
      }

      if (baseLexer.getTokenType() == COLON) {
        if (counter.lexerWasAdvanced(baseLexer)) {
          addToken(baseLexer.getTokenStart(), ATTR_NAME);
          advanceLexer(baseLexer);
          return tokenizeTagExpression(baseLexer, TokenSet.create(TAG_START, RIGHT_BRACE, CLOSE_TAG, COMMA, SINGLE_QUOTE, DOUBLE_QUOTE));
        }
        else {
          baseLexer.advance();
          return tokenizeExpression(baseLexer, until, type, counter);
        }
      }
      else if (baseLexer.getTokenType() == TAG_START) {
        addToken(baseLexer.getTokenStart(), type);
        tokenizeTag(baseLexer);
        return true;
      }
      else {
        if (counter.lexerWasAdvanced(baseLexer) || baseLexer.getTokenType() == null) { //lexer was advanced
          addToken(baseLexer.getTokenStart(), type);
        }
      }
    }

    return completeTokenizeExpression(baseLexer);
  }

  private static void countBrackets(Lexer baseLexer, CounterHelper counter) {
    final IElementType tokenType = baseLexer.getTokenType();
    if (tokenType == LEFT_BRACKET) {
      counter.bracketNesting++;
    }
    else if (tokenType == LEFT_BRACE) {
      counter.braceNesting++;
    }
    else if (tokenType == LEFT_PAREN) {
      counter.parenNesting++;
    }
    else if (tokenType == RIGHT_BRACKET && counter.bracketNesting > 0) {
      counter.bracketNesting--;
    }
    else if (tokenType == RIGHT_BRACE && counter.braceNesting > 0) {
      counter.braceNesting--;
    }
    else if (tokenType == RIGHT_PAREN && counter.parenNesting > 0) {
      counter.parenNesting--;
    }
  }

  private boolean tokenizeExpressionWithQuotes(Lexer baseLexer, TokenSet until, IElementType type, CounterHelper counter, IElementType quote) {
    if (baseLexer.getTokenType() == quote) {
      advanceQuotes(baseLexer, type, quote);
    }
    if (counter.hasNestingBrackets()) {
      return tokenizeExpression(baseLexer, until, type, counter);
    }
    if (baseLexer.getTokenType() == COMMA) {
      addToken(baseLexer.getTokenStart(), type);
      completeTokenizeExpression(baseLexer);
      return true;
    }
    if (until.contains(baseLexer.getTokenType())) {
      addToken(baseLexer.getTokenStart(), type);
      completeTokenizeExpression(baseLexer);
    }
    else {
      tokenizeExpression(baseLexer, until, type);
    }
    return true;
  }

  private boolean completeTokenizeExpression(Lexer baseLexer) {
    if (baseLexer.getTokenType() == COMMA) {
      advanceLexer(baseLexer);
      tokenizeTagExpression(baseLexer);
    }
    else if (baseLexer.getTokenType() == TAG_START) {
      tokenizeTag(baseLexer);
    }
    else {
      mergeToTagEnd(baseLexer);
    }

    return true;
  }

  private boolean advanceQuotes(Lexer baseLexer, IElementType type, IElementType quote) {
    baseLexer.advance();
    if (baseLexer.getTokenType() == null) {
      advanceAs(baseLexer, BAD_CHARACTER);
    }
    else if (currentOrSkipTokens(baseLexer, TokenSet.create(quote))) {
      if (baseLexer.getTokenType() == null) {
        advanceAs(baseLexer, BAD_CHARACTER);
      }
      else {
        baseLexer.advance();
        if (baseLexer.getTokenType() == null) {
          addToken(baseLexer.getTokenStart(), type);
        }
      }
      return true;
    }
    return false;
  }

  private void mergeToTagEnd(Lexer baseLexer) {
    if (baseLexer.getTokenType() == RIGHT_BRACE || baseLexer.getTokenType() == CLOSE_TAG) {
      advanceTagEnd(baseLexer);
    }
    else {
      addWhiteSpaces(baseLexer);

      if (baseLexer.getTokenType() == RIGHT_BRACE || baseLexer.getTokenType() == CLOSE_TAG) {
        advanceTagEnd(baseLexer);
      }
    }
  }

  private void advanceTagEnd(Lexer baseLexer) {
    if (baseLexer.getTokenType() == RIGHT_BRACE) {
      advanceAs(baseLexer, TAG_END);
    }
    else {
      advanceLexer(baseLexer);
    }
  }

  private void mergeToRightBrace(Lexer baseLexer, @NotNull IElementType expressionType, @Nullable IElementType replaceBraceWith) {
    mergeToRightBrace(baseLexer, TokenSet.create(RIGHT_BRACE), expressionType, replaceBraceWith, new CounterHelper());
  }

  protected void mergeTo(Lexer baseLexer,
                         @NotNull TokenSet mergeTo,
                         @NotNull IElementType expressionType,
                         @Nullable IElementType replaceBraceWith) {
    if (mergeTo.contains(baseLexer.getTokenType())) {
      advanceWithReplace(baseLexer, replaceBraceWith);
    }
    else if (skipTokens(baseLexer, mergeTo)) {
      addToken(baseLexer.getTokenStart(), expressionType);
      advanceWithReplace(baseLexer, replaceBraceWith);
    }
  }

  protected void mergeToRightBrace(Lexer baseLexer,
                                   @NotNull TokenSet mergeTo,
                                   @NotNull IElementType expressionType,
                                   @Nullable IElementType replaceBraceWith,
                                   @NotNull CounterHelper counter) {

    final IElementType tokenType = baseLexer.getTokenType();
    if (mergeTo.contains(tokenType)) {
      advanceWithReplace(baseLexer, replaceBraceWith);
    }
    else {
      if (currentOrSkipTokens(baseLexer, TokenSet.orSet(mergeTo, TokenSet.create(RIGHT_BRACE, LEFT_BRACE)))) {
        final IElementType currentTokenType = baseLexer.getTokenType();

        if (currentTokenType == LEFT_BRACE || (currentTokenType == RIGHT_BRACE && counter.braceNesting > 0)) {
          countBrackets(baseLexer, counter);
          baseLexer.advance();
          mergeToRightBrace(baseLexer, mergeTo, expressionType, replaceBraceWith, counter);
        }
        else {
          addToken(baseLexer.getTokenStart(), expressionType);
          if (baseLexer.getTokenType() != null) {
            advanceWithReplace(baseLexer, replaceBraceWith);
          }
        }
      }
      else {
        advanceWithReplace(baseLexer, replaceBraceWith);
      }
    }
  }

  private void advanceWithReplace(Lexer baseLexer, IElementType replaceBraceWith) {
    if (replaceBraceWith != null) {
      advanceAs(baseLexer, replaceBraceWith);
    }
    else {
      advanceLexer(baseLexer);
    }
  }

  private void addWhiteSpaces(Lexer baseLexer) {
    while (baseLexer.getTokenType() == WHITE_SPACE) {
      addToken(WHITE_SPACE);
      baseLexer.advance();
    }
  }

  private void tokenizeComment(Lexer baseLexer) {
    advanceLexer(baseLexer);
    mergeTo(baseLexer, TokenSet.create(COMMENT_END), COMMENT_TEXT, null);
  }

  private void tokenizeAction(Lexer baseLexer) {
    advanceLexer(baseLexer);
    mergeToRightBrace(baseLexer, ACTION_SCRIPT, ACTION_END);
  }

  private void tokenizeMessage(Lexer baseLexer) {
    advanceLexer(baseLexer);
    tokenizeMessage(baseLexer, new CounterHelper(baseLexer.getTokenEnd()));
  }

  private void tokenizeMessage(Lexer baseLexer, @NotNull CounterHelper counter) {
    //addWhiteSpaces(baseLexer);
    final IElementType tokenType = baseLexer.getTokenType();

    if (tokenType == RIGHT_BRACE) {
      if (counter.lexerWasAdvanced(baseLexer)) addToken(baseLexer.getTokenStart(), MESSAGE_TEXT);
      advanceWithReplace(baseLexer, MESSAGE_END);
    }
    else {
      final TokenSet set = TokenSet.create(COMMA, RIGHT_BRACE, LEFT_BRACE, SINGLE_QUOTE);
      if (currentOrSkipTokens(baseLexer, set)) {
        final IElementType currentTokenType = baseLexer.getTokenType();

        if (currentTokenType == SINGLE_QUOTE) {
          if (baseLexer.getTokenType() == SINGLE_QUOTE) {
            if (advanceQuotes(baseLexer, MESSAGE_TEXT, SINGLE_QUOTE) && baseLexer.getTokenType() != BAD_CHARACTER) {
              tokenizeMessage(baseLexer, counter);
              return;
            }
            else {
              advanceAs(baseLexer, BAD_CHARACTER);
              return;
            }
          }
        }
        else if (currentTokenType == LEFT_BRACE || (currentTokenType == RIGHT_BRACE && counter.braceNesting > 0)) {
          countBrackets(baseLexer, counter);
          baseLexer.advance();
          tokenizeMessage(baseLexer, counter);
        }
        else if (currentTokenType == COMMA && !counter.hasNestingBrackets()) {
          if (counter.lexerWasAdvanced(baseLexer)) addToken(baseLexer.getTokenStart(), MESSAGE_TEXT);
          advanceLexer(baseLexer);
          tokenizeMessage(baseLexer, new CounterHelper(baseLexer.getTokenEnd()));
        }
        else {
          addToken(baseLexer.getTokenStart(), MESSAGE_TEXT);
          if (baseLexer.getTokenType() != null) {
            advanceWithReplace(baseLexer, MESSAGE_END);
            return;
          }
        }
      }
      else {
        if (baseLexer.getTokenType() != null) advanceWithReplace(baseLexer, MESSAGE_END);
      }
    }
  }

  public static PlayLexer createLexer() {
    return new PlayLexer(new _PlayLexer());
  }

  private static class CounterHelper {
    public int parenNesting = 0;
    public int bracketNesting = 0;
    public int braceNesting = 0;

    public int lastLexerPosition = -1;

    private CounterHelper() {
    }

    CounterHelper(int lexerPosition) {
      lastLexerPosition = lexerPosition;
    }

    public boolean lexerWasAdvanced(Lexer baseLexer) {
      return lastLexerPosition != baseLexer.getTokenEnd();
    }

    public boolean hasNestingBrackets() {
      return parenNesting != 0 || braceNesting != 0 || bracketNesting != 0;
    }
  }
}
