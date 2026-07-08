package com.intellij.lang.puppet.editing;

import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler;
import com.intellij.lang.puppet.PuppetTokenTypes;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/**
 * @author Anna Bulenkova
 */
public class PuppetQuoteHandler extends SimpleTokenSetQuoteHandler {
  public PuppetQuoteHandler() {
    super(PuppetTokenTypes.STRING, PuppetTokenTypes.SINGLE_QUOTED_STRING, PuppetTokenTypes.DOUBLE_QUOTED_STRING);
  }

  @Override
  public boolean isOpeningQuote(@NotNull HighlighterIterator iterator, int offset) {
    if (isInsideLiteral(iterator)) {
      int start = iterator.getStart();
      return offset == start;
    }
    else {
      final IElementType tokenType = iterator.getTokenType();
      return tokenType == PuppetTokenTypes.DOUBLE_QUOTED_STRING_START;
    }
  }

  @Override
  public boolean isClosingQuote(@NotNull HighlighterIterator iterator, int offset) {
    if (isInsideLiteral(iterator)) {
      int start = iterator.getStart();
      int end = iterator.getEnd();
      return end - start >= 1 && offset == end - 1;
    }
    else {
      return iterator.getTokenType() == PuppetTokenTypes.DOUBLE_QUOTED_STRING_END;
    }
  }

  @Override
  public boolean hasNonClosedLiteral(@NotNull Editor editor, @NotNull HighlighterIterator iterator, int offset) {
    final Document document = editor.getDocument();
    int lineEndOffset = document.getLineEndOffset(document.getLineNumber(offset));
    if (offset < lineEndOffset) {
      final CharSequence charSequence = document.getCharsSequence();
      final char openQuote = charSequence.charAt(offset);
      final int nextCharOffset = offset + 1;
      if (nextCharOffset < lineEndOffset && charSequence.charAt(nextCharOffset) == openQuote) {
        return true;
      }
      for (int i = nextCharOffset + 1; i < lineEndOffset; i++) {
        if (charSequence.charAt(i) == openQuote) {
          return false;
        }
      }
    }
    return true;
  }
}
