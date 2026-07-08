package com.intellij.lang.puppet.ide;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.lang.puppet.PuppetTokenTypes;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Anna Bulenkova
 */
public class PuppetBraceMatcher implements PairedBraceMatcher, PuppetTokenTypes {
  private static final BracePair[] ourBracePairs =
    {
      new BracePair(VARIABLE_LBRACE, VARIABLE_RBRACE, true),
      new BracePair(LBRACE, RBRACE, true),
      new BracePair(LBRACK, RBRACK, false),
      new BracePair(LISTSTART, RBRACK, false),
      new BracePair(LPAREN, RPAREN, false),
      new BracePair(LCOLLECT, RCOLLECT, false),
      new BracePair(LLCOLLECT, RRCOLLECT, false),
      new BracePair(VAR_INTERPOLATION_START, VAR_INTERPOLATION_END, true),
    };

  @Override
  public BracePair @NotNull [] getPairs() {
    return ourBracePairs;
  }

  @Override
  public boolean isPairedBracesAllowedBeforeType(final @NotNull IElementType lbraceType, final @Nullable IElementType contextType) {
    return true;
  }

  @Override
  public int getCodeConstructStart(final PsiFile file, final int openingBraceOffset) {
    return openingBraceOffset;
  }
}
