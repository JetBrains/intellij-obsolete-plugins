package com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.raw

import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCellType
import com.intellij.bigdatatools.notebooks.zeppelin.psi.ZeppelinTypes
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.InterpreterTypes
import com.intellij.psi.tree.IElementType

object RawElementTypes : InterpreterTypes {
  override val id: String = "raw"
  override val code = listOf<String>()
  override val marker: IElementType = ZeppelinTypes.RAW_MARKER
  override val source: IElementType = ZeppelinTypes.RAW_SOURCE
  override val cellType: NotebookCellType = NotebookCellType.RAW
}