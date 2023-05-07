package com.intellij.play.language.support;

import com.intellij.codeInsight.highlighting.PairedBraceMatcherAdapter;
import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.play.language.PlayElementTypes;
import com.intellij.play.language.PlayLanguage;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayPairedBraceMatcher extends PairedBraceMatcherAdapter {
  public static final BracePair[] PAIRS = new BracePair[]{
      new BracePair(PlayElementTypes.ACTION_START, PlayElementTypes.ACTION_END, true),
      new BracePair(PlayElementTypes.ACTION_DOUBLE_START, PlayElementTypes.ACTION_END, true),
      new BracePair(PlayElementTypes.EL_START, PlayElementTypes.EL_END, true),
      new BracePair(PlayElementTypes.COMMENT_START, PlayElementTypes.COMMENT_END, true),
      new BracePair(PlayElementTypes.MESSAGE_START, PlayElementTypes.MESSAGE_END, true),
      new BracePair(PlayElementTypes.SCRIPT_START, PlayElementTypes.SCRIPT_END, true),
      new BracePair(PlayElementTypes.SCRIPT_START_TOO, PlayElementTypes.SCRIPT_END, true)
  };

  public PlayPairedBraceMatcher() {
    super(new MyPairedBraceMatcher(), PlayLanguage.INSTANCE);
  }

  private static class MyPairedBraceMatcher implements PairedBraceMatcher {
    @Override
    public BracePair @NotNull [] getPairs() {
      return PAIRS;
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(@NotNull final IElementType lbraceType, @Nullable final IElementType type) {
      return true;
    }

    @Override
    public int getCodeConstructStart(final PsiFile file, int openingBraceOffset) {
      return openingBraceOffset;
    }
  }
}