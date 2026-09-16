// This is a generated file. Not intended for manual editing.
package com.intellij.bigdatatools.notebooks.zeppelin.psi;

import com.intellij.bigdatatools.notebooks.core.impl.psi.impl.*;
import com.intellij.bigdatatools.zeppelin.psi.ZeppelinElementType;
import com.intellij.bigdatatools.zeppelin.psi.ZeppelinTokenType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;

public interface ZeppelinTypes {

  IElementType CELL = new ZeppelinElementType("CELL");
  IElementType CELL_MAGIC = new ZeppelinElementType("CELL_MAGIC");
  IElementType CELL_MARKER = new ZeppelinElementType("CELL_MARKER");
  IElementType NOTEBOOK = new ZeppelinElementType("NOTEBOOK");
  IElementType SOURCE = new ZeppelinElementType("SOURCE");
  IElementType STEM_CELL = new ZeppelinElementType("STEM_CELL");

  IElementType ANY = new ZeppelinTokenType("ANY");
  IElementType CODE_MARKER = new ZeppelinTokenType("CODE_MARKER");
  IElementType CODE_SOURCE = new ZeppelinTokenType("CODE_SOURCE");
  IElementType HIVE_MARKER = new ZeppelinTokenType("HIVE_MARKER");
  IElementType HIVE_SOURCE = new ZeppelinTokenType("HIVE_SOURCE");
  IElementType MAGIC = new ZeppelinTokenType("MAGIC");
  IElementType MARKDOWN_MARKER = new ZeppelinTokenType("MARKDOWN_MARKER");
  IElementType MARKDOWN_SOURCE = new ZeppelinTokenType("MARKDOWN_SOURCE");
  IElementType RAW_MARKER = new ZeppelinTokenType("RAW_MARKER");
  IElementType RAW_SOURCE = new ZeppelinTokenType("RAW_SOURCE");
  IElementType SCALA_MARKER = new ZeppelinTokenType("SCALA_MARKER");
  IElementType SCALA_SOURCE = new ZeppelinTokenType("SCALA_SOURCE");
  IElementType KOTLIN_MARKER = new ZeppelinTokenType("KOTLIN_MARKER");
  IElementType KOTLIN_SOURCE = new ZeppelinTokenType("KOTLIN_SOURCE");
  IElementType SH_MARKER = new ZeppelinTokenType("SH_MARKER");
  IElementType SH_SOURCE = new ZeppelinTokenType("SH_SOURCE");
  IElementType SQL_MARKER = new ZeppelinTokenType("SQL_MARKER");
  IElementType SQL_SOURCE = new ZeppelinTokenType("SQL_SOURCE");
  IElementType PYTHON_MARKER = new ZeppelinTokenType("PYTHON_MARKER");
  IElementType PYTHON_SOURCE = new ZeppelinTokenType("PYTHON_SOURCE");


  final class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == CELL) {
        return new PsiCellImpl(node);
      }
      else if (type == CELL_MAGIC) {
        return new PsiCellMagicImpl(node);
      }
      else if (type == CELL_MARKER) {
        return new PsiCellMarkerImpl(node);
      }
      else if (type == NOTEBOOK) {
        return new PsiNotebookImpl(node);
      }
      else if (type == SOURCE) {
        return new PsiSourceImpl(node);
      }
      else if (type == STEM_CELL) {
        return new PsiStemCellImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
