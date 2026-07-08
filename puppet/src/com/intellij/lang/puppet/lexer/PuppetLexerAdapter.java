package com.intellij.lang.puppet.lexer;

import com.intellij.lang.puppet.PuppetTokenTypes;
import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.Lexer;
import com.intellij.lexer.MergeFunction;
import com.intellij.lexer.MergingLexerAdapter;
import com.intellij.openapi.project.Project;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.Nullable;

/**
 * @author Anna Bulenkova
 */
public class PuppetLexerAdapter extends MergingLexerAdapter implements PuppetTokenTypes {
  private static final TokenSet TOKENS_TO_MERGE = TokenSet.create(
    COMMENT, TokenType.WHITE_SPACE);
  private final MergeFunction myMergeFunction;

  public PuppetLexerAdapter(@Nullable Project project) {
    super(new FlexAdapter(new PuppetLexer(project)), TOKENS_TO_MERGE);
    myMergeFunction = new PuppetLexerMergeFunction(super.getMergeFunction());
  }

  @Override
  public MergeFunction getMergeFunction() {
    return myMergeFunction;
  }

  private static class PuppetLexerMergeFunction implements MergeFunction {
    private final MergeFunction myDelegate;

    PuppetLexerMergeFunction(MergeFunction delegate) {
      myDelegate = delegate;
    }

    @Override
    public IElementType merge(IElementType type, Lexer originalLexer) {
      if (type == DOUBLE_QUOTED_STRING_START && originalLexer.getTokenType() == DOUBLE_QUOTED_STRING_END) {
        originalLexer.advance();
        return DOUBLE_QUOTED_STRING;
      }
      return myDelegate.merge(type, originalLexer);
    }
  }
}
