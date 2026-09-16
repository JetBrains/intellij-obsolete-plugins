package com.intellij.bigdatatools.zeppelin.notebook.interpreter

import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCellType
import com.intellij.psi.tree.IElementType

interface InterpreterTypes {
  val id: String
  val cellType: NotebookCellType
  val source: IElementType
  val marker: IElementType
  val code: List<String>
}