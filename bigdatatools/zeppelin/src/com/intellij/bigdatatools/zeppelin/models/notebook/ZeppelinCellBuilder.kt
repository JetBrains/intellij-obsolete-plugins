package com.intellij.bigdatatools.zeppelin.models.notebook

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellStatus
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookSchema

object ZeppelinCellBuilder {
  fun createFromText(notebook: ZeppelinNotebook, text: String, id: String = ""): ZeppelinCell {
    val cellJson = createCellJson()
    val schema = NotebookSchema
    cellJson.addProperty(schema.cellText, text)
    cellJson.addProperty(schema.cellId, id)
    return ZeppelinCell(notebook, cellJson)
  }

  fun createFromSource(notebook: ZeppelinNotebook, source: String, id: String = ""): ZeppelinCell {
    val cell = ZeppelinCell(notebook, createCellJson())
    cell.runWithMuteNotification {
      cell.source = source
      cell.id = id
    }
    return cell
  }

  private fun createCellJson(): JsonObject {
    val cellJson = JsonObject()
    val schema = NotebookSchema
    val jsonSettings = Gson().toJsonTree(ParagraphSettings())
    cellJson.add(schema.cellSettings, jsonSettings)
    cellJson.add(schema.cellApps, JsonArray())
    cellJson.addProperty(schema.cellStatus, CellStatus.READY.name)
    return cellJson
  }
}