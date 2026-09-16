package com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.database.sparksql

import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCellType
import com.intellij.bigdatatools.notebooks.zeppelin.psi.ZeppelinTypes
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.InterpreterTypes
import com.intellij.psi.tree.IElementType

object SparkSqlElementTypes : InterpreterTypes {
  override val id: String = "sql"
  override val code = listOf(id, "athena")
  override val marker: IElementType = ZeppelinTypes.SQL_MARKER
  override val source: IElementType = ZeppelinTypes.SQL_SOURCE
  override val cellType: NotebookCellType = NotebookCellType("SQL")
}