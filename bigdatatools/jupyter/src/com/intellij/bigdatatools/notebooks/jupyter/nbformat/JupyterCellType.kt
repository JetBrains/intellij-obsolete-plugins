// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.notebooks.jupyter.nbformat

import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCellType

object JupyterCellType {
  private val RAW = NotebookCellType.RAW
  private val UNDEFINED = NotebookCellType.UNDEFINED
  private val CODE = NotebookCellType.CODE
  val MARKDOWN = NotebookCellType("MARKDOWN")
  val CODE_OR_MAGIC = NotebookCellType("CODE_OR_MAGIC")
  val MAGIC = NotebookCellType("MAGIC")

  fun values(): List<NotebookCellType> = listOf(RAW, CODE, CODE_OR_MAGIC, MAGIC, MARKDOWN, UNDEFINED)
  fun number(cellType: NotebookCellType) = values().indexOf(cellType)
  fun value(index: Int): NotebookCellType = values()[index]

}