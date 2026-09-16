package com.intellij.bigdatatools.notebooks.core.impl.nbformat

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.intellij.bigdatatools.notebooks.core.api.nbformat.BasicNotebook
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellAdded
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellChanged
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellRemoved
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NoteChangeEvent
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookChangeListener
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookEvent
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.concurrency.ThreadingAssertions
import java.io.Reader
import java.util.TreeMap

abstract class BasicNotebookImpl(reader: Reader) : NotebookMetadataAwareBase(), BasicNotebook {
  var lastChangeTimestamp = 10L
    private set
  private var notCommittedChanges = mutableListOf<NotebookEvent>()
  private var isNoteNotificationEnabled: Boolean = true
  internal var isNoteChangeEnabled = false

  override val json: JsonObject = gson.fromJson(reader, JsonElement::class.java).asJsonObject

  private val cellsModel: MutableList<NotebookCell> by lazy {
    getCellElements()
      .map { createCell(it) }
      .toMutableList()
  }

  override val cells: List<NotebookCell>
    get() = cellsModel.toList()

  override fun performModification(body: () -> Unit) = invokeAndWaitIfNeeded {
    startModificationSection()
    body()
    endModificationSection()
  }

  override fun startModificationSection() {
    ThreadingAssertions.assertEventDispatchThread()
    assertIsCommitted()
    isNoteChangeEnabled = true
  }

  override fun endModificationSection() {
    isNoteChangeEnabled = false
    commitChanges()
  }

  private fun assertIsCommitted() {
    assert(notCommittedChanges.isEmpty()) {
      "There are not committed note events\n" +
      notCommittedChanges.joinToString("\n") { it.toString() }
    }
  }

  private fun commitChanges() {
    val changes = synchronized(this) {
      notCommittedChanges.toList().also {
        notCommittedChanges.clear()
      }
    }
    changes.lastOrNull()?.isLastInBatch = true

    changes.forEach {
      notifyListeners(it)
    }
  }

  override fun updateCell(newCell: NotebookCell, index: Int) {
    val oldCell = cells[index]

    isNoteNotificationEnabled = false
    removeCell(index)
    addCell(newCell, index)
    isNoteNotificationEnabled = true

    val changedFields = newCell.getDifference(oldCell)
    if (changedFields.isEmpty())
      return
    addEvent(CellChanged(this, newCell, changedFields))
  }

  override fun addCell(cell: NotebookCell, index: Int) {
    if (index > cells.size) {
      logger.warn("Trying to add cell to index $index outside of cells range ${cells.size}")
      return
    }
    cell.setSourceNote(this)
    //update model
    cellsModel.add(index, cell)
    //update shadow json
    addCellToJson(cell.asJsonTree(), index)

    if (!isNoteNotificationEnabled) return
    addEvent(CellAdded(this, cell, index))
  }

  override fun moveCell(cell: NotebookCell, toIndex: Int) {
    removeCell(cell.indexInNote)
    addCell(cell, toIndex)
  }

  override fun removeCell(cell: NotebookCell) = removeCell(cell.indexInNote)

  override fun removeCell(index: Int) {
    val cellsElement = json[schema.cellFieldName]
    if (cellsElement == null || !cellsElement.isJsonArray) return

    if (index >= cells.size) {
      logger.warn("Trying to remove cell from index $index outside of cells range ${cells.size}")
      return
    }

    val removedCell = cellsModel.removeAt(index)
    cellsElement.asJsonArray.remove(index)

    if (!isNoteNotificationEnabled) return
    addEvent(CellRemoved(this, removedCell, index))
  }

  override fun replace(newNote: BasicNotebook) {
    if (cells.size == newNote.cells.size && asSource() == newNote.asSource()) {
      cells.zip(newNote.cells).forEach { (origin, change) ->
        origin.update(change)
      }
      return
    }

    repeat(cells.size) {
      removeCell(0)
    }
    newNote.cells.withIndex().forEach {
      it.value.setSourceNote(this)
      addCell(it.value, it.index)
    }

    val oldJson = json
    val newJson = newNote.json
    val keys = (oldJson.keySet() + newJson.keySet()) - schema.cellFieldName
    keys.forEach {
      if (newJson.has(it)) {
        json.add(it, newJson.get(it))
      }
      else {
        json.remove(it)
      }
    }
  }

  private fun getCellElements(): Sequence<JsonObject> {
    val cellsElement = json[schema.cellFieldName]
    if (cellsElement == null || !cellsElement.isJsonArray) {
      error("Can't find proper cells element: $cellsElement")
    }
    return cellsElement.asJsonArray
      .asSequence()
      .filterIsInstance<JsonObject>()
  }

  protected abstract fun createCell(content: JsonObject): NotebookCell

  fun addEvent(cellEvent: NotebookEvent) {
    val lastEvent = notCommittedChanges.lastOrNull()
    if (cellEvent is CellChanged && lastEvent is CellChanged && lastEvent.cell == cellEvent.cell)
      lastEvent.changedFields += cellEvent.changedFields
    else
      notCommittedChanges.add(cellEvent)

    updateTimeStamp()
  }

  private fun addCellToJson(cellJson: JsonElement, index: Int) {
    // Since gson isn't able to add element to arbitrary place in the array we have to create copy here
    val oldCells = getCellElements().toList()
    val newCells = JsonArray(oldCells.size + 1)
    for (i in 0 until index) {
      newCells.add(oldCells[i])
    }
    newCells.add(cellJson)
    for (i in index until oldCells.size) {
      newCells.add(oldCells[i])
    }
    json.add(schema.cellFieldName, newCells)
  }

  private val notebookChangeListener = mutableListOf<NotebookChangeListener>()

  override fun removeNotebookChangeListener(notebookChangeListener: NotebookChangeListener) {
    this.notebookChangeListener.remove(notebookChangeListener)
  }

  override fun addNotebookChangeListener(notebookChangeListener: NotebookChangeListener) {
    this.notebookChangeListener.add(notebookChangeListener)
  }

  private fun notifyListeners(notebookEvent: NotebookEvent) {
    notebookChangeListener.forEach {
      try {
        it.onEvent(notebookEvent)
      }
      catch (e: Throwable) {
        logger.error(e)
      }
    }
  }

  override fun getCellByOffset(offset: Int): NotebookCell? = cells.find { it.textRange.contains(offset) }

  override fun updateTimeStamp(): Long {
    lastChangeTimestamp += 1
    return lastChangeTimestamp
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is BasicNotebookImpl) return false

    if (json != other.json) return false

    return true
  }

  fun notifyChangedNote(changedFields: Set<String>) {
    if (changedFields.isEmpty()) return

    ThreadingAssertions.assertEventDispatchThread()
    assert(this.isNoteChangeEnabled) {
      "Please edit notebook model just in notebook#performModification section"
    }
    addEvent(NoteChangeEvent(this, changedFields))
  }

  override fun hashCode(): Int = json.hashCode()

  override fun toString(): String {
    val json = gson.toJson(json)
    val map = gson.fromJson<Any>(json, TreeMap::class.java)
    return gson.toJson(map)
  }

  companion object {
    private val logger = Logger.getInstance(this::class.java)

    val gson: Gson = GsonBuilder()
      .disableHtmlEscaping()
      .serializeNulls()
      .setPrettyPrinting()
      .create()
  }
}