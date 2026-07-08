package com.intellij.lang.puppet.highlighter;

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

/**
 * @author Anna Bulenkova
 */
public final class PuppetSyntaxHighlighterColors {

  public static final TextAttributesKey BLOCK_COMMENT =
    createTextAttributesKey("PUPPET_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT);
  public static final TextAttributesKey REGEX =
    createTextAttributesKey("PUPPET_REGEX", DefaultLanguageHighlighterColors.TEMPLATE_LANGUAGE_COLOR);
  public static final TextAttributesKey VARIABLE =
    createTextAttributesKey("PUPPET_VARIABLE", DefaultLanguageHighlighterColors.INSTANCE_FIELD);
  public static final TextAttributesKey VARIABLE_INTERPOLATION =
    createTextAttributesKey("PUPPET_VARIABLE_INTERPOLATION", DefaultLanguageHighlighterColors.INSTANCE_FIELD);
  public static final TextAttributesKey VARIABLE_INTERPOLATION_TAGS =
    createTextAttributesKey("PUPPET_VARIABLE_INTERPOLATION_TAGS", VARIABLE_INTERPOLATION);
  public static final TextAttributesKey ESCAPE_SEQUENCE =
    createTextAttributesKey("PUPPET_ESCAPE_SEQUENCE", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE);
  public static final TextAttributesKey RESOURCE_REFERENCE =
    createTextAttributesKey("PUPPET_RESOURCE_REFERENCE", DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL);
  public static final TextAttributesKey SQ_STRING =
    createTextAttributesKey("PUPPET_SQ_STRING", DefaultLanguageHighlighterColors.STRING);
  public static final TextAttributesKey KEYWORD =
    createTextAttributesKey("PUPPET_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
  public static final TextAttributesKey DIGIT =
    createTextAttributesKey("PUPPET_NUMBER", DefaultLanguageHighlighterColors.NUMBER);
  public static final TextAttributesKey DQ_STRING =
    createTextAttributesKey("PUPPET_STRING", DefaultLanguageHighlighterColors.STRING);
  public static final TextAttributesKey OPERATION_SIGN =
    createTextAttributesKey("PUPPET_OPERATION_SIGN", DefaultLanguageHighlighterColors.OPERATION_SIGN);
  public static final TextAttributesKey PARENTHS =
    createTextAttributesKey("PUPPET_PARENTH", DefaultLanguageHighlighterColors.PARENTHESES);
  public static final TextAttributesKey BRACKETS =
    createTextAttributesKey("PUPPET_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS);
  public static final TextAttributesKey BRACES =
    createTextAttributesKey("PUPPET_BRACES", DefaultLanguageHighlighterColors.BRACES);
  public static final TextAttributesKey COMMA = createTextAttributesKey("PUPPET_COMMA", DefaultLanguageHighlighterColors.COMMA);
  public static final TextAttributesKey DOT = createTextAttributesKey("PUPPET_DOT", DefaultLanguageHighlighterColors.DOT);
  public static final TextAttributesKey SEMIC =
    createTextAttributesKey("PUPPET_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON);
  public static final TextAttributesKey BAD_CHARACTER =
    createTextAttributesKey("PUPPET_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);
  public static final TextAttributesKey NAME =
    createTextAttributesKey("PUPPET_NAME", DefaultLanguageHighlighterColors.CLASS_NAME);
  public static final TextAttributesKey HEREDOC_TAGS =
    createTextAttributesKey("PUPPET_HEREDOC_TAGS", DefaultLanguageHighlighterColors.IDENTIFIER);
}
