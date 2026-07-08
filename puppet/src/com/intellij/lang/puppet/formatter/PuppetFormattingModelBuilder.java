package com.intellij.lang.puppet.formatter;

import com.intellij.formatting.FormattingContext;
import com.intellij.formatting.FormattingModel;
import com.intellij.formatting.FormattingModelBuilder;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.formatter.DocumentBasedFormattingModel;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

import static com.intellij.lang.puppet.PuppetTokenTypes.ANONYMOUS_BLOCK;
import static com.intellij.lang.puppet.PuppetTokenTypes.APPENDS;
import static com.intellij.lang.puppet.PuppetTokenTypes.BRACED_CASE_OPTS_BLOCK;
import static com.intellij.lang.puppet.PuppetTokenTypes.BRACED_RESOURCE_BY_CLASSNAME_CONTENTS_BLOCK;
import static com.intellij.lang.puppet.PuppetTokenTypes.BRACED_SELECTOR_VALUES_BLOCK;
import static com.intellij.lang.puppet.PuppetTokenTypes.BRACED_STATEMENTS_BLOCK;
import static com.intellij.lang.puppet.PuppetTokenTypes.CASE_OPTION;
import static com.intellij.lang.puppet.PuppetTokenTypes.COLON;
import static com.intellij.lang.puppet.PuppetTokenTypes.COMMA;
import static com.intellij.lang.puppet.PuppetTokenTypes.DELETES;
import static com.intellij.lang.puppet.PuppetTokenTypes.DIV;
import static com.intellij.lang.puppet.PuppetTokenTypes.DOT;
import static com.intellij.lang.puppet.PuppetTokenTypes.EQUALS;
import static com.intellij.lang.puppet.PuppetTokenTypes.EXPRESSION_UNARY;
import static com.intellij.lang.puppet.PuppetTokenTypes.FARROW;
import static com.intellij.lang.puppet.PuppetTokenTypes.GREATEREQUAL;
import static com.intellij.lang.puppet.PuppetTokenTypes.GREATERTHAN;
import static com.intellij.lang.puppet.PuppetTokenTypes.IN_EDGE;
import static com.intellij.lang.puppet.PuppetTokenTypes.IN_EDGE_SUB;
import static com.intellij.lang.puppet.PuppetTokenTypes.ISEQUAL;
import static com.intellij.lang.puppet.PuppetTokenTypes.LBRACE;
import static com.intellij.lang.puppet.PuppetTokenTypes.LESSEQUAL;
import static com.intellij.lang.puppet.PuppetTokenTypes.LESSTHAN;
import static com.intellij.lang.puppet.PuppetTokenTypes.LPAREN;
import static com.intellij.lang.puppet.PuppetTokenTypes.LSHIFT;
import static com.intellij.lang.puppet.PuppetTokenTypes.MATCH;
import static com.intellij.lang.puppet.PuppetTokenTypes.MINUS;
import static com.intellij.lang.puppet.PuppetTokenTypes.MODULO;
import static com.intellij.lang.puppet.PuppetTokenTypes.NOMATCH;
import static com.intellij.lang.puppet.PuppetTokenTypes.NOT;
import static com.intellij.lang.puppet.PuppetTokenTypes.NOTEQUAL;
import static com.intellij.lang.puppet.PuppetTokenTypes.OUT_EDGE;
import static com.intellij.lang.puppet.PuppetTokenTypes.OUT_EDGE_SUB;
import static com.intellij.lang.puppet.PuppetTokenTypes.PARAMETER;
import static com.intellij.lang.puppet.PuppetTokenTypes.PARENTHESIZED_EXPRESSIONS_LIST_BLOCK;
import static com.intellij.lang.puppet.PuppetTokenTypes.PARENTHESIZED_PARAMETERS_LIST_BLOCK;
import static com.intellij.lang.puppet.PuppetTokenTypes.PARROW;
import static com.intellij.lang.puppet.PuppetTokenTypes.PIPED_PARAMETERS_LIST_BLOCK;
import static com.intellij.lang.puppet.PuppetTokenTypes.PLUS;
import static com.intellij.lang.puppet.PuppetTokenTypes.QMARK;
import static com.intellij.lang.puppet.PuppetTokenTypes.RBRACE;
import static com.intellij.lang.puppet.PuppetTokenTypes.REGULAR_NAME_WRAPPER;
import static com.intellij.lang.puppet.PuppetTokenTypes.RESOURCE_INSTANCE_DECLARATION;
import static com.intellij.lang.puppet.PuppetTokenTypes.RESOURCE_LIKE_CLASS_DECLARATION_BLOCK;
import static com.intellij.lang.puppet.PuppetTokenTypes.RPAREN;
import static com.intellij.lang.puppet.PuppetTokenTypes.RSHIFT;
import static com.intellij.lang.puppet.PuppetTokenTypes.SELECTOR_VALUE;
import static com.intellij.lang.puppet.PuppetTokenTypes.SEMIC;
import static com.intellij.lang.puppet.PuppetTokenTypes.TIMES;

/**
 * @author Anna Bulenkova
 */
public class PuppetFormattingModelBuilder implements FormattingModelBuilder {
  private static final TokenSet NO_SPACE_BEFORE_SPACE_AFTER_TOKENSET = TokenSet.create(
    COMMA,
    COLON,
    SEMIC
  );

  private static final TokenSet SPACES_AROUND_TOKENSET = TokenSet.create(
    FARROW,
    PARROW,
    APPENDS,
    DELETES,
    NOTEQUAL,
    QMARK,
    ISEQUAL,
    GREATEREQUAL,
    LESSEQUAL,
    LSHIFT,
    RSHIFT,
    MATCH,
    NOMATCH,
    OUT_EDGE,
    IN_EDGE,
    IN_EDGE_SUB,
    OUT_EDGE_SUB,
    EQUALS,
    GREATERTHAN,
    LESSTHAN,
    MODULO,
    DIV,
    PLUS,
    MINUS,
    TIMES,
    PARENTHESIZED_PARAMETERS_LIST_BLOCK,
    PIPED_PARAMETERS_LIST_BLOCK
  );

  private static final TokenSet POSSIBLE_UNARY_OPS = TokenSet.create(
    MINUS,
    NOT,
    TIMES
  );

  private static final TokenSet NO_SPACES_AROUND_TOKENSET = TokenSet.create(
    DOT
  );

  private static final TokenSet SPACE_BEFORE_TOKENSET = TokenSet.create(
    BRACED_STATEMENTS_BLOCK,
    BRACED_RESOURCE_BY_CLASSNAME_CONTENTS_BLOCK,
    RESOURCE_LIKE_CLASS_DECLARATION_BLOCK,
    BRACED_CASE_OPTS_BLOCK,
    BRACED_SELECTOR_VALUES_BLOCK,
    ANONYMOUS_BLOCK
  );

  @Override
  public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {
    final PsiFile file = formattingContext.getContainingFile();

    CodeStyleSettings settings = formattingContext.getCodeStyleSettings();
    final PuppetFormattingBlock rootBlock = new PuppetFormattingBlock(
      formattingContext.getNode(),
      createSpacingBuilder(settings),
      new PuppetIndentProcessor(),
      new PuppetAlignmentProcessor(),
      new PuppetWrappingProcessor(file)
    );

    return new DocumentBasedFormattingModel(rootBlock, file.getProject(), settings, file.getFileType(), file);
  }

  private static SpacingBuilder createSpacingBuilder(final CodeStyleSettings settings) {
    return new SpacingBuilder(settings, PuppetLanguage.INSTANCE)
      // todo: catch heredocs on getSpacing
      // parameters
      .between(LPAREN, PARAMETER).lineBreakInCodeIf(true)
      .between(PARAMETER, RPAREN).lineBreakInCodeIf(true)
      .betweenInside(COMMA, PARAMETER, PARENTHESIZED_PARAMETERS_LIST_BLOCK).lineBreakInCodeIf(true)
      .betweenInside(PARAMETER, COMMA, PARENTHESIZED_PARAMETERS_LIST_BLOCK).lineBreakInCodeIf(false)
      .betweenInside(COMMA, RPAREN, PARENTHESIZED_PARAMETERS_LIST_BLOCK).lineBreakInCodeIf(true)

      // resource instance fixme unsured rules are commented, should be configurable or described by puppetlabs
      //.between(COLON, RESOURCE_ARGUMENTS_LIST).lineBreakInCodeIf(true)
      //.between(COMMA, ARGUMENT).lineBreakInCodeIf(true)
      .between(SEMIC, RESOURCE_INSTANCE_DECLARATION).lineBreakInCodeIf(true)
      //.between(RESOURCE_INSTANCE_DECLARATION, RBRACE).lineBreakInCodeIf(true)
      // resource defaults
      //.between(LBRACE, RESOURCE_ARGUMENTS_LIST).lineBreakInCodeIf(true)
      //.between(RESOURCE_ARGUMENTS_LIST,RBRACE).lineBreakInCodeIf(true)

      // selectors
      .between(LBRACE, SELECTOR_VALUE).lineBreakInCodeIf(true)
      .between(COMMA, SELECTOR_VALUE).lineBreakInCodeIf(true)
      .between(SELECTOR_VALUE, RBRACE).lineBreakInCodeIf(true)

      // case
      .between(LBRACE, CASE_OPTION).lineBreakInCodeIf(true)
      .between(CASE_OPTION, CASE_OPTION).lineBreakInCodeIf(true)
      .between(CASE_OPTION, RBRACE).lineBreakInCodeIf(true)

      // calls
      .afterInside(LPAREN, PARENTHESIZED_EXPRESSIONS_LIST_BLOCK).none()
      .beforeInside(RPAREN, PARENTHESIZED_EXPRESSIONS_LIST_BLOCK).none()

      // misc
      .between(REGULAR_NAME_WRAPPER, PARENTHESIZED_EXPRESSIONS_LIST_BLOCK).none()
      .between(LBRACE, RBRACE).spaces(0)
      .withinPair(LBRACE, RBRACE).spaceIf(true)
      .after(RBRACE).spaceIf(true)
      .before(SPACE_BEFORE_TOKENSET).spaceIf(true)
      .between(CASE_OPTION, CASE_OPTION).spaceIf(true)
      .around(NO_SPACES_AROUND_TOKENSET).none()
      .before(NO_SPACE_BEFORE_SPACE_AFTER_TOKENSET).none().after(NO_SPACE_BEFORE_SPACE_AFTER_TOKENSET).spaceIf(true)
      .afterInside(POSSIBLE_UNARY_OPS, EXPRESSION_UNARY)
      .none() // this MUST be before spaces around tokenset check, because - and * are ambiguous and first wins
      .around(SPACES_AROUND_TOKENSET).spaces(1)
      ;
  }
}
