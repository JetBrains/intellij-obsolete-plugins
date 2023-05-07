package com.intellij.play.language.formatter;

import com.intellij.lang.ASTNode;
import com.intellij.play.language.PlayElementTypes;
import com.intellij.psi.formatter.WhiteSpaceFormattingStrategyAdapter;
import org.jetbrains.annotations.NotNull;

public class PlayWhiteSpaceFormattingStrategy extends WhiteSpaceFormattingStrategyAdapter {
  @Override
  public boolean containsWhitespacesOnly(@NotNull ASTNode node) {
    return (node.getElementType() == PlayElementTypes.TEMPLATE_TEXT) && node.getText().trim().length() == 0;
  }
}
