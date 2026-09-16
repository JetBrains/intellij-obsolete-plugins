package com.intellij.bigdatatools.notebooks.core.api.nbformat

import com.intellij.bigdatatools.notebooks.core.impl.nbformat.NotebookMetadataAware
import com.intellij.lang.Language

interface NotebookCellBuilder : NotebookMetadataAware {
  var cellType: NotebookCellType
  var language: Language?
  var source: CharSequence
  fun build(): NotebookCell
}