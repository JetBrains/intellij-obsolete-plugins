package com.intellij.lang.puppet.lexer;

import com.intellij.psi.tree.IElementType;

public class LexerTokenDescriptor {
  private final int myStartOffset;
  private final int myEndOffset;
  private final IElementType myTokenType;

  public LexerTokenDescriptor(int startOffset, int endOffset, IElementType tokenType) {
    myStartOffset = startOffset;
    myEndOffset = endOffset;
    myTokenType = tokenType;
  }

  public int getStartOffset() {
    return myStartOffset;
  }

  public int getEndOffset() {
    return myEndOffset;
  }

  public IElementType getTokenType() {
    return myTokenType;
  }

  @Override
  public String toString() {
    return "TokenDescriptor: " + getStartOffset() + " - " + getEndOffset() + "; " + getTokenType();
  }
}
