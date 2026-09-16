package com.intellij.bigdatatools.zeppelin.psi;

import com.intellij.psi.tree.IElementType;

public interface ZeppelinTemplateTypes {
  IElementType NEWLINE = new ZeppelinTokenType("NEWLINE");
  IElementType OUTER = new ZeppelinTokenType("OUTER");
}
