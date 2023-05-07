package com.intellij.play.language.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public class PlayTagManipulator extends AbstractElementManipulator<PlayTag> {
  @Override
  public PlayTag handleContentChange(@NotNull PlayTag playTag, @NotNull TextRange range, String newContent) throws IncorrectOperationException {
    if (isNameElement(playTag, range)) {
      final StringBuilder replacement = new StringBuilder( playTag.getName());
      final int valueOffset = playTag.getNameElement().getTextRange().getStartOffset() - playTag.getTextOffset();

      replacement.replace(
        range.getStartOffset() - valueOffset,
        range.getEndOffset() - valueOffset,
        newContent
      );
      playTag.setName(replacement.toString());
      return playTag;
    }

    return playTag;
  }

  private static boolean isNameElement(PlayTag tag, TextRange range) {
    PsiElement nameElement = tag.getNameElement();
    if (nameElement != null) {
      TextRange tagTextRange = tag.getTextRange();
      TextRange nameElementRange = nameElement.getTextRange();
      return TextRange.create(nameElementRange.getStartOffset()-tagTextRange.getStartOffset(), nameElementRange.getEndOffset() - tagTextRange.getStartOffset()).contains(range);
    }

    return false;
  }
}
