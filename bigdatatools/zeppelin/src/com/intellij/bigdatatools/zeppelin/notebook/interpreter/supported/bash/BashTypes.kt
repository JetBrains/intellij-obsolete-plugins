package com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.bash

import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCellType
import com.intellij.bigdatatools.notebooks.zeppelin.psi.ZeppelinTypes
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.InterpreterTypes
import com.intellij.psi.tree.IElementType

object BashTypes : InterpreterTypes {
  override val code = listOf("sh")
  override val id: String = "sh"
  override val source: IElementType = ZeppelinTypes.SH_SOURCE
  override val marker: IElementType = ZeppelinTypes.SH_MARKER
  override val cellType: NotebookCellType = NotebookCellType("SH")
}