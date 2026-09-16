package com.intellij.bigdatatools.notebooks.core.api

import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.openapi.util.Key

object NotebookKeys {
  val NOTEBOOK_VIRTUAL_FILE = Key.create<NotebookVirtualFile>("NOTEBOOK_VIRTUAL_FILE_KEY")
}