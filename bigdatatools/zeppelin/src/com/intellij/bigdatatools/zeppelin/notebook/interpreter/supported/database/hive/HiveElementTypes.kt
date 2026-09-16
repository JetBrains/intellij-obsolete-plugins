package com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.database.hive

import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCellType
import com.intellij.bigdatatools.notebooks.zeppelin.psi.ZeppelinTypes
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.InterpreterTypes
import com.intellij.psi.tree.IElementType

object HiveElementTypes : InterpreterTypes {
  override val id: String = "hive"
  override val code = listOf(id)
  override val source: IElementType = ZeppelinTypes.HIVE_SOURCE
  override val marker: IElementType = ZeppelinTypes.HIVE_MARKER
  override val cellType: NotebookCellType = NotebookCellType("HIVE")
}