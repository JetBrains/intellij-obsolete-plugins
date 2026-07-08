package com.intellij.lang.puppet.psi.manipulators;

import com.intellij.lang.puppet.psi.PuppetQuotedString;
import com.intellij.lang.puppet.util.PuppetElementFactory;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public class PuppetQuotedTextManipulator extends AbstractElementManipulator<PuppetQuotedString> {
  @Override
  public @NotNull TextRange getRangeInElement(@NotNull PuppetQuotedString element) {
    TextRange range = element.getTextRange();
    if (range.getLength() < 2) {
      return TextRange.EMPTY_RANGE;
    }

    return TextRange.create(1, range.getLength() - 1);
  }

  @Override
  public PuppetQuotedString handleContentChange(@NotNull PuppetQuotedString element, @NotNull TextRange range, String newContent)
    throws IncorrectOperationException {
    String oldText = element.getText();
    assert range.getStartOffset() > 0 && range.getEndOffset() < oldText.length() : "String range must be inside quotes, got " + range;

    String newText = range.replace(oldText, newContent);
    if (newText.length() > 2) {
      TextRange contentRange = TextRange.create(1, newText.length() - 1);
      if (newText.charAt(0) == '\'') {
        newText = contentRange.replace(newText, PuppetElementFactory.smartEscapeChar(contentRange.substring(newText), '\''));
      }
      else if (newText.charAt(0) == '"') {
        newText = contentRange.replace(newText, PuppetElementFactory.smartEscapeChar(contentRange.substring(newText), '"'));
      }
    }
    return (PuppetQuotedString)element.replace(PuppetElementFactory.createQuotedStringElement(element.getProject(), newText));
  }
}
