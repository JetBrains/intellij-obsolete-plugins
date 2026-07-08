package com.intellij.lang.puppet.lexer;

import com.intellij.lang.puppet.PuppetTokenTypes;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

/**
 * @author Anna Bulenkova
 */
public interface PuppetTokenTypeSets extends PuppetTokenTypes {
  TokenSet KEYWORDS = PuppetLexerKeywords.getAllKeywordsTokenset();

  TokenSet DQ_STRINGS = TokenSet.create(DOUBLE_QUOTED_STRING,
                                        DOUBLE_QUOTED_STRING_START,
                                        DOUBLE_QUOTED_STRING_MIDDLE,
                                        DOUBLE_QUOTED_STRING_END);

  TokenSet OPERATORS = TokenSet.create(PLUS,
                                       MINUS,
                                       TIMES,
                                       EQUALS,
                                       NOTEQUAL,
                                       FARROW,
                                       IN_EDGE,
                                       OUT_EDGE,
                                       IN_EDGE_SUB,
                                       OUT_EDGE_SUB,
                                       MATCH,
                                       NOMATCH,
                                       LSHIFT,
                                       RSHIFT,
                                       PARROW
  );


  TokenSet FUNCTION_STATEMENTS = TokenSet.create(FUNCTION_CALL_STATEMENT, FUNCTION_CALL_EXPRESSION, INCLUDE_CLASS_STATEMENT);

  TokenSet DIGITS = TokenSet.create(INTEGER_LITERAL_WITHOUTQ, FLOAT_LITERAL);

  TokenSet VARIABLE_INTERPOLATION_TAGS = TokenSet.create(VAR_INTERPOLATION_START, VAR_INTERPOLATION_END);

  TokenSet HEREDOC_TAGS = TokenSet.create(HEREDOC_END_TAG, HEREDOC_ENDING);

  IElementType BAD_CHARACTER = TokenType.BAD_CHARACTER;

  TokenSet VARIABLE_TOKENSET = TokenSet.create(PuppetTokenTypes.VAR_WRAPPER);

  TokenSet PARAMETERS_HOLDERS_TOKENSET = TokenSet.create(PARENTHESIZED_PARAMETERS_LIST_BLOCK, PIPED_PARAMETERS_LIST_BLOCK);

  TokenSet PRODUCES_CONSUMES_TOKENSET = TokenSet.create(
    PRODUCES_STATEMENT, CONSUMES_STATEMENT
  );

  /**
   * Elements containing names
   */
  TokenSet RESOURCE_NAME_HOLDERS = TokenSet.create(
    QUOTED_TEXT,
    DEFAULT_WRAPPER,
    VAR_WRAPPER,
    REGULAR_NAME_WRAPPER
  );

  /**
   * Name elements containers
   */
  TokenSet RESOURCE_NAME_CONTAINERS = TokenSet.create(
    ARRAY
  );

  TokenSet COMMENTS = TokenSet.create(COMMENT, HEREDOC_BODY, HEREDOC_BODY_QQ, HEREDOC_ENDING);
  TokenSet WHITESPACE_OR_COMMENTS = TokenSet.orSet(TokenSet.WHITE_SPACE, COMMENTS);
}
