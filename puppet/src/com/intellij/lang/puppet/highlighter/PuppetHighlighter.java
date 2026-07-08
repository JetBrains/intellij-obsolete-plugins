package com.intellij.lang.puppet.highlighter;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.BAD_CHARACTER;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.COMMA;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.COMMENT;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.DIGITS;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.DOLLAR;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.DOT;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.DQ_STRINGS;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.ESCAPE_SEQUENCE;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.HEREDOC_BODY;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.HEREDOC_TAGS;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.KEYWORDS;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.LBRACE;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.LBRACK;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.LPAREN;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.NAME;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.OPERATORS;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.RBRACE;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.RBRACK;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.REGEX;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.RPAREN;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.SEMIC;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.SINGLE_QUOTED_STRING;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.VARIABLE_INTERPOLATION_TAGS;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.VARIABLE_LBRACE;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.VARIABLE_NAME;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.VARIABLE_RBRACE;

/**
 * @author Anna Bulenkova
 */
public class PuppetHighlighter extends SyntaxHighlighterBase {
  protected final @Nullable Project myProject;
  private static final @NotNull Map<IElementType, TextAttributesKey> ourMap = new HashMap<>();

  public PuppetHighlighter(@Nullable Project project) {
    myProject = project;
  }

  static {
    safeMap(ourMap, KEYWORDS, PuppetSyntaxHighlighterColors.KEYWORD);
    safeMap(ourMap, NAME, PuppetSyntaxHighlighterColors.NAME);
    safeMap(ourMap, DQ_STRINGS, PuppetSyntaxHighlighterColors.DQ_STRING);
    safeMap(ourMap, DIGITS, PuppetSyntaxHighlighterColors.DIGIT);
    safeMap(ourMap, REGEX, PuppetSyntaxHighlighterColors.REGEX);
    safeMap(ourMap, VARIABLE_INTERPOLATION_TAGS, PuppetSyntaxHighlighterColors.VARIABLE_INTERPOLATION_TAGS);
    safeMap(ourMap, OPERATORS, PuppetSyntaxHighlighterColors.OPERATION_SIGN);
    //safeMap(ourMap, RESOURCE_REFERENCE, PuppetSyntaxHighlighterColors.RESOURCE_REFERENCE);
    safeMap(ourMap, DOLLAR, PuppetSyntaxHighlighterColors.VARIABLE);
    safeMap(ourMap, VARIABLE_LBRACE, PuppetSyntaxHighlighterColors.VARIABLE);
    safeMap(ourMap, VARIABLE_NAME, PuppetSyntaxHighlighterColors.VARIABLE);
    safeMap(ourMap, VARIABLE_RBRACE, PuppetSyntaxHighlighterColors.VARIABLE);
    safeMap(ourMap, SINGLE_QUOTED_STRING, PuppetSyntaxHighlighterColors.SQ_STRING);
    safeMap(ourMap, LPAREN, PuppetSyntaxHighlighterColors.PARENTHS);
    safeMap(ourMap, RPAREN, PuppetSyntaxHighlighterColors.PARENTHS);
    safeMap(ourMap, LBRACE, PuppetSyntaxHighlighterColors.BRACES);
    safeMap(ourMap, RBRACE, PuppetSyntaxHighlighterColors.BRACES);
    safeMap(ourMap, LBRACK, PuppetSyntaxHighlighterColors.BRACKETS);
    safeMap(ourMap, RBRACK, PuppetSyntaxHighlighterColors.BRACKETS);
    safeMap(ourMap, COMMA, PuppetSyntaxHighlighterColors.COMMA);
    safeMap(ourMap, DOT, PuppetSyntaxHighlighterColors.DOT);
    safeMap(ourMap, SEMIC, PuppetSyntaxHighlighterColors.SEMIC);
    safeMap(ourMap, COMMENT, PuppetSyntaxHighlighterColors.BLOCK_COMMENT);
    safeMap(ourMap, BAD_CHARACTER, PuppetSyntaxHighlighterColors.BAD_CHARACTER);
    safeMap(ourMap, ESCAPE_SEQUENCE, PuppetSyntaxHighlighterColors.ESCAPE_SEQUENCE);
    safeMap(ourMap, HEREDOC_TAGS, PuppetSyntaxHighlighterColors.HEREDOC_TAGS);
    safeMap(ourMap, HEREDOC_BODY, PuppetSyntaxHighlighterColors.SQ_STRING);
  }

  @Override
  public @NotNull Lexer getHighlightingLexer() {
    return new PuppetHighlightingLexer(myProject);
  }

  @Override
  public TextAttributesKey @NotNull [] getTokenHighlights(final IElementType tokenType) {
    return pack(ourMap.get(tokenType));
  }
}
