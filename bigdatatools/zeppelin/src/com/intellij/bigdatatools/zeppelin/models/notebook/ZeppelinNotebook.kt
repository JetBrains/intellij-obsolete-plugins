package com.intellij.bigdatatools.zeppelin.models.notebook

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.stream.JsonWriter
import com.intellij.bigdatatools.notebooks.core.api.NotebookConstants
import com.intellij.bigdatatools.notebooks.core.api.nbformat.BasicNotebook
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCellType
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookSchema
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.BasicNotebookImpl
import com.intellij.bigdatatools.zeppelin.models.connection.AngularRemoveResponse
import com.intellij.bigdatatools.zeppelin.models.connection.AngularUpdateResponse
import java.io.Reader
import java.io.StringWriter

class ZeppelinNotebook(reader: Reader) : BasicNotebookImpl(reader) {
  override val delimiter: String = NotebookConstants.PARAGRAPH_DELIMITER
  override val schema = NotebookSchema

  var id: String
    get() = json[NotebookSchema.noteId].asString
    set(value) {
      if (value == id) return

      json.addProperty(NotebookSchema.noteId, value)
      notifyChangedNote(setOf(NotebookSchema.noteId))
    }

  override val cells: List<ZeppelinCell>
    get() = super.cells.map { it as ZeppelinCell }

  override val stemCell: ZeppelinCell?
    get() = null

  var name: String
    get() = json[NotebookSchema.noteName].asString
    set(value) {
      if (name == value)
        return
      json.addProperty(NotebookSchema.noteName, value)
      notifyChangedNote(setOf(NotebookSchema.noteName))
    }

  var path: String?
    get() = if (json[NotebookSchema.notePath] != null)
      json[NotebookSchema.notePath].asString
    else
      null
    set(value) {
      if (path == value)
        return
      json.addProperty(NotebookSchema.notePath, value)
      notifyChangedNote(setOf(NotebookSchema.notePath))
    }

  val clearName: String
    get() = name.removePrefix("/").removeSuffix("/")

  override fun addNewCell(source: String, index: Int) {
    val zeppelinCell = ZeppelinCellBuilder.createFromSource(this, source)
    addCell(zeppelinCell, index)
  }

  override fun buildCell(text: String, cellType: NotebookCellType): NotebookCell =
    ZeppelinCellBuilder.createFromText(this, text)

  override fun asSource(): String {
    val builder = StringBuilder()

    cells.asSequence()
      .map { it.source }
      .joinTo(builder, "\n")

    return builder.toString().replace("\r\n", "\n")
  }

  override fun replace(newNote: BasicNotebook) {
    if (newNote !is ZeppelinNotebook) error("Zeppelin note must be replaced just by Zeppelin note")
    id = newNote.id
    name = newNote.name

    super.replace(newNote)
  }

  override fun updateCell(newCell: NotebookCell, index: Int) {
    newCell as ZeppelinCell
    cells[index].update(newCell)
  }

  override fun asJson(): String {
    val writer = StringWriter()
    val jupyterStyleWriter = JsonWriter(writer).apply {
      setIndent(" ")
    }
    gson.toJson(json.deepCopy(), jupyterStyleWriter)
    return writer.toString()
  }

  override fun createCell(content: JsonObject) = ZeppelinCell(this, content)

  override fun createCellFromText(textMarker: String) = ZeppelinCellBuilder.createFromText(this, textMarker)

  fun getSimilarCell(cell: ZeppelinCell) = cells.firstOrNull { it == cell }
                                           ?: cells.firstOrNull { it.id == cell.id && it.id.isNotEmpty() }

  fun removeAngularObject(angularObject: AngularRemoveResponse) {
    if (!json.has(NotebookSchema.angularObjects))
      json.add(NotebookSchema.angularObjects, JsonObject())
    val angularObjects = json.getAsJsonObject(NotebookSchema.angularObjects)
    angularObjects.entrySet().forEach { topics ->
      val objects = topics.value.asJsonArray
      val found = objects.indexOfFirst { it.asJsonObject.get("name").asString == angularObject.name }
      if (found > -1)
        objects.remove(found)
    }
  }

  fun updateAngularObjects(newAngularObject: AngularUpdateResponse) {
    if (!json.has(NotebookSchema.angularObjects))
      json.add(NotebookSchema.angularObjects, JsonObject())
    val angularObjects = json.getAsJsonObject(NotebookSchema.angularObjects)
    if (!angularObjects.has(newAngularObject.interpreterGroupId))
      angularObjects.add(newAngularObject.interpreterGroupId, JsonArray())
    val interpreterObjects = angularObjects.getAsJsonArray(newAngularObject.interpreterGroupId) ?: return
    val exists = interpreterObjects.find { it.asJsonObject.get("name") == newAngularObject.angularObject["name"] }
    val newAngularJson = Gson().toJsonTree(newAngularObject.angularObject).asJsonObject
    if (exists == newAngularJson)
      return

    interpreterObjects.remove(exists)
    interpreterObjects.add(newAngularJson)
  }
}