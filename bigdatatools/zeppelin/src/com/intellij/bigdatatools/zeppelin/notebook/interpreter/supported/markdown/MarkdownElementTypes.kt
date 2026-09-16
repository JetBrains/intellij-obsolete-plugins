package com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.markdown

import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCellType
import com.intellij.bigdatatools.notebooks.jupyter.nbformat.JupyterCellType
import com.intellij.bigdatatools.notebooks.zeppelin.psi.ZeppelinTypes
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.InterpreterTypes
import com.intellij.psi.tree.IElementType

object MarkdownElementTypes : InterpreterTypes {
  override val id: String = "markdown"
  override val code = listOf("md")
  override val marker: IElementType = ZeppelinTypes.MARKDOWN_MARKER
  override val source: IElementType = ZeppelinTypes.MARKDOWN_SOURCE
  override val cellType: NotebookCellType = JupyterCellType.MARKDOWN
}