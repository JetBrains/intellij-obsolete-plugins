package com.intellij.bigdatatools.zeppelin.notebook.interpreter

import com.intellij.bigdatatools.notebooks.jupyter.nbformat.JupyterCellType

object ZeppelinCellTypes {
  private val cellTypes = (JupyterCellType.values() + ZeppelinSupportLanguages.CELL_TYPES).distinct()
  fun values() = cellTypes
}