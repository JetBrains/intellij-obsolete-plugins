package com.intellij.bigdatatools.notebooks.core.impl.editor.external

import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell

abstract class NotebookModificationException: Exception()

data class CellLockedException(val cell: NotebookCell,val index: Int): NotebookModificationException()