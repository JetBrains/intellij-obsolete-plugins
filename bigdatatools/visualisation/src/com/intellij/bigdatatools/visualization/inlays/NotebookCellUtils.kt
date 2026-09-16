package com.intellij.bigdatatools.visualization.inlays

import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.NotebookCellBase
import com.intellij.charts.utils.getAsJsonObjectOrNull
import com.intellij.charts.utils.getOrCreateJsonObject
import com.intellij.charts.utils.optBoolean

object NotebookCellUtils {
  // ToDo Copy/paste from ZeppelinNotebookSchema.
  const val tableHide: String = "tableHide"
  const val cellConfig: String = "config"
  const val cellStatus: String = "status"
}

var NotebookCell.tableHide: Boolean
  get() = asJsonTree().getAsJsonObjectOrNull(NotebookCellUtils.cellConfig)?.optBoolean(NotebookCellUtils.tableHide) ?: false
  set(value) {
    if (tableHide != value) {
      val cell = this as? NotebookCellBase ?: return
      cell.note?.performModification {
        notifyChangeFields(listOf(NotebookCellUtils.cellConfig))
        asJsonTree().getOrCreateJsonObject(NotebookCellUtils.cellConfig).addProperty(NotebookCellUtils.tableHide, value)
      }
    }
  }