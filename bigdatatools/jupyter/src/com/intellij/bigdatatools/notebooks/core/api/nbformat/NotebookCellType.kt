package com.intellij.bigdatatools.notebooks.core.api.nbformat

data class NotebookCellType(val name: String) {
  companion object {
    val CODE = NotebookCellType("CODE")
    val RAW = NotebookCellType("RAW")
    val UNDEFINED = NotebookCellType("UNDEFINED")
  }
}
