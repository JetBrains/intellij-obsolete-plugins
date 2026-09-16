package com.intellij.bigdatatools.zeppelin.models.notebook

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookSchema

object ZeppelinNotebookBuilder {
  fun createNotebook(name: String = "", id: String = "", cells: List<ZeppelinCell> = emptyList()): ZeppelinNotebook {
    val jsonNote = JsonObject()
    val jsonCells = JsonArray()
    cells.forEach {
      jsonCells.add(it.asJsonTree())
    }
    jsonNote.add(NotebookSchema.cellFieldName, jsonCells)
    jsonNote.add(NotebookSchema.noteName, JsonPrimitive(name))
    jsonNote.add(NotebookSchema.noteId, JsonPrimitive(id))
    return createFromJson(jsonNote.toString())
  }

  fun createFromJson(jsonString: String): ZeppelinNotebook = ZeppelinNotebook(jsonString.reader())
}