package com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.scala

import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCellType
import com.intellij.bigdatatools.notebooks.zeppelin.psi.ZeppelinTypes
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.InterpreterTypes
import com.intellij.psi.tree.IElementType

object SparkElementTypes : InterpreterTypes {
  override val id: String = "scala"
  override val code: List<String> = listOf("")
  override val marker: IElementType = ZeppelinTypes.SCALA_MARKER
  override val source: IElementType = ZeppelinTypes.SCALA_SOURCE
  override val cellType: NotebookCellType = NotebookCellType("SPARK")
}