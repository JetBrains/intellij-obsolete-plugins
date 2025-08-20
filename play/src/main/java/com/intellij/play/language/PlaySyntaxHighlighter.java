package com.intellij.play.language;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.JspHighlighterColors;
import com.intellij.openapi.editor.XmlHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.play.language.lexer.PlayLexer;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static com.intellij.play.language.PlayElementTypes.*;

public final class PlaySyntaxHighlighter extends SyntaxHighlighterBase {
  private static final Map<IElementType,TextAttributesKey> ourMap;

  static {
    ourMap = new HashMap<>();
    fillMap(ourMap, DefaultLanguageHighlighterColors.PARENTHESES, LEFT_PAREN, RIGHT_PAREN);
    fillMap(ourMap, DefaultLanguageHighlighterColors.BRACES, LEFT_BRACE, RIGHT_BRACE);
    fillMap(ourMap, DefaultLanguageHighlighterColors.BRACKETS, LEFT_BRACKET, RIGHT_BRACKET);
    fillMap(ourMap, DefaultLanguageHighlighterColors.STRING, STRING_TEXT);
    fillMap(ourMap, DefaultLanguageHighlighterColors.STRING, QUOTES.getTypes());
    fillMap(ourMap, DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE, CHAR_ESCAPE);
    fillMap(ourMap, DefaultLanguageHighlighterColors.COMMA, COMMA);

    fillMap(ourMap, JspHighlighterColors.JSP_ACTION_AND_DIRECTIVE_NAME, EL_START, EL_START,
            EL_END, RIGHT_BRACE);
    fillMap(ourMap, JspHighlighterColors.JSP_ACTION_AND_DIRECTIVE_NAME,
            ACTION_START, ACTION_DOUBLE_START, ACTION_END,
            SCRIPT_START, SCRIPT_START_TOO, SCRIPT_END,
            MESSAGE_START, MESSAGE_END, TAG_START, END_TAG_START, TAG_END);

    fillMap(ourMap, JspHighlighterColors.JSP_COMMENT, COMMENT_TEXT, COMMENT_START, COMMENT_END);
    fillMap(ourMap, JspHighlighterColors.JSP_SCRIPTING_BACKGROUND, ACTION_SCRIPT);

    fillMap(ourMap, XmlHighlighterColors.HTML_TAG_NAME, TAG_NAME);
    fillMap(ourMap, JspHighlighterColors.JSP_ACTION_AND_DIRECTIVE_NAME, TAG_EXPRESSION);
  }

  @Override
  @NotNull
  public Lexer getHighlightingLexer() {
    return PlayLexer.createLexer();
  }

  @Override
  public TextAttributesKey @NotNull [] getTokenHighlights(final IElementType tokenType) {
    return pack(ourMap.get(tokenType), XmlHighlighterColors.XML_TAG);
  }
}
