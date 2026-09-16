package com.intellij.bigdatatools.visualization.inlays

import com.google.gson.JsonObject
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell

object NotebookInlaySettingsZeppelinAdapter {

  fun save(settings: NotebookInlaySettings, cell: NotebookCell) {
    val inlayMetadata = JsonObject()
    // Collapsed and size settings we are saving to inlay metadata
    saveSizeAndCollapsedState(settings, inlayMetadata)
    cell.note?.performModification {
      cell.setMetadata("inlay", inlayMetadata)
    }
  }

  private fun saveSizeAndCollapsedState(settings: NotebookInlaySettings, inlayMetadata: JsonObject) {

    if (settings.height != null) {
      val size = JsonObject()
      size.addProperty("height", settings.height)
      inlayMetadata.add("size", size)
    }

    if (settings.collapsed != null) {
      inlayMetadata.addProperty("collapsed", settings.collapsed)
    }
  }
}