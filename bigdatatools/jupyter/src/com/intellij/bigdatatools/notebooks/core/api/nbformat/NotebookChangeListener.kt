// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.notebooks.core.api.nbformat

interface NotebookChangeListener {
  fun onEvent(notebookEvent: NotebookEvent)
}

abstract class NotebookEvent {
  var isLastInBatch: Boolean = false
}

data class NoteChangeEvent(val notebook: BasicNotebook, var changedFields: Set<String>) : NotebookEvent() {
  override fun toString() = "Note Changed. changed fields: ${changedFields}"
}

abstract class NotebookCellEvent : NotebookEvent() {
  abstract val notebook: BasicNotebook
  abstract val cell: NotebookCell
}

data class CellChanged(override val notebook: BasicNotebook,
                       override val cell: NotebookCell,
                       var changedFields: Set<String>) : NotebookCellEvent() {
  override fun toString() = "Cell Changed. Id - ${cell.id}, index - ${cell.indexInNote}, changed fields - ${changedFields}"
}

data class CellAdded(override val notebook: BasicNotebook,
                     override val cell: NotebookCell,
                     val index: Int) : NotebookCellEvent() {
  override fun toString() = "Cell Added. Id - ${cell.id}, index - ${index}, text - ${cell.text}"
}

data class CellRemoved(override val notebook: BasicNotebook,
                       override val cell: NotebookCell,
                       val index: Int) : NotebookCellEvent() {
  override fun toString() = "Cell Removed. Id - ${cell.id}, index - ${index}, text - ${cell.text}"
}
