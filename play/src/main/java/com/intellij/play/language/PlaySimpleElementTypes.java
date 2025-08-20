/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.play.language;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

public interface PlaySimpleElementTypes extends TokenType {

  // QUOTES
  PlayTokenType SINGLE_QUOTE = new PlayTokenType("SINGLE_QUOTE");

  PlayTokenType R_SINGLE_QUOTE = new PlayTokenType("r'");
  PlayTokenType DOUBLE_QUOTE = new PlayTokenType("\"");

  PlayTokenType R_DOUBLE_QUOTE = new PlayTokenType("r\"");

  PlayTokenType TERMINATING_WHITE_SPACE = new PlayTokenType("TERMINATING_WHITE_SPACE");
  TokenSet QUOTES = TokenSet.create(SINGLE_QUOTE, R_SINGLE_QUOTE, DOUBLE_QUOTE, R_DOUBLE_QUOTE);

  IElementType DOLLAR = new IElementType("DOLLAR", PlayLanguage.INSTANCE);
  IElementType SHARP = new IElementType("SHARP", PlayLanguage.INSTANCE);
  IElementType AMPERSAND = new IElementType("AMPERSAND", PlayLanguage.INSTANCE);
  IElementType QUOTE = new IElementType("QUOTE", PlayLanguage.INSTANCE);
  IElementType BACK_QUOTE = new IElementType("BACK_QUOTE", PlayLanguage.INSTANCE);
  IElementType PERIOD = new IElementType("PERIOD", PlayLanguage.INSTANCE);
  IElementType DOUBLE_PERIOD = new IElementType("DOUBLE_PERIOD", PlayLanguage.INSTANCE);
  IElementType SOLIDUS = new IElementType("SOLIDUS", PlayLanguage.INSTANCE);
  IElementType DOUBLE_COLON = new IElementType("DOUBLE_COLON", PlayLanguage.INSTANCE);
  IElementType AT = new IElementType("AT", PlayLanguage.INSTANCE);
  IElementType QUESTION_MARK = new IElementType("QUESTION_MARK", PlayLanguage.INSTANCE);
  IElementType UNDERSCORE = new IElementType("UNDERSCORE", PlayLanguage.INSTANCE);
  IElementType VERTICAL_BAR = new IElementType("VERTICAL_BAR", PlayLanguage.INSTANCE);
  IElementType ASTERISK = new IElementType("ASTERISK", PlayLanguage.INSTANCE);
  IElementType PERCENT = new IElementType("PERCENT", PlayLanguage.INSTANCE);

  IElementType JUST_GT = new IElementType("JUST_GT", PlayLanguage.INSTANCE);

  PlayTokenType LEFT_BRACKET = new PlayTokenType("[");
  PlayTokenType LEFT_BRACE = new PlayTokenType("{");
  PlayTokenType LEFT_PAREN = new PlayTokenType("(");
  PlayTokenType RIGHT_BRACKET = new PlayTokenType("]");
  PlayTokenType RIGHT_PAREN = new PlayTokenType(")");
  PlayTokenType RIGHT_BRACE = new PlayTokenType("}");

  PlayTokenType COMMA = new PlayTokenType("COMMA");
  PlayTokenType COLON = new PlayTokenType("COLON");
  PlayTokenType SEMICOLON = new PlayTokenType(";");

  PlayTokenType OR = new PlayTokenType("OR");
  PlayTokenType AND = new PlayTokenType("AND");
  PlayTokenType EQ = new PlayTokenType("=");
  PlayTokenType NEQ = new PlayTokenType("NEQ");

  PlayTokenType LT = new PlayTokenType("LT");
  PlayTokenType LTE = new PlayTokenType("LTE");
  PlayTokenType GT = new PlayTokenType("GT");
  PlayTokenType GTE = new PlayTokenType("GTE");

  PlayTokenType RANGE = new PlayTokenType("RANGE");

  PlayTokenType EXCLAM = new PlayTokenType("EXCLAM");
  PlayTokenType DOUBLE_QUESTION = new PlayTokenType("DOUBLE_QUESTION");
  PlayTokenType QUESTION = new PlayTokenType("QUESTION");

  PlayTokenType BOOLEAN = new PlayTokenType("BOOLEAN");

  PlayTokenType CHAR_ESCAPE = new PlayTokenType("CHAR_ESCAPE");
}
