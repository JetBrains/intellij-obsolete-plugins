package com.intellij.bigdatatools.notebooks.core.impl.nbformat

import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellChanged
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.openapi.util.TextRange
import com.intellij.util.concurrency.ThreadingAssertions
import java.util.TreeMap

abstract class NotebookCellBase(protected var sourceNotebook: BasicNotebookImpl) : NotebookMetadataAwareBase(), NotebookCell {
  private var notifyOnChange: Boolean = true

  private val cachedIndexInNote = CachedNoteField(::note) {
    sourceNotebook.cells.indexOf(this)
  }

  /**
   * Return -1 if note does not contain the cell
   */
  override val indexInNote: Int
    get() = cachedIndexInNote.getValue()

  override fun getDifference(anotherCell: NotebookCell): Set<String> {
    val oldJson = asJsonTree().asJsonObject
    val newJson = anotherCell.asJsonTree().asJsonObject
    val allKeys = oldJson.keySet() + newJson.keySet()
    return allKeys.filter {
      oldJson.get(it) != newJson.get(it)
    }.toSet()
  }

  val isInNote: Boolean
    get() = sourceNotebook.cells.contains(this)

  override fun setSourceNote(sourceNote: BasicNotebookImpl) {
    sourceNotebook = sourceNote
  }


  fun performMutedChange(runnable: () -> Unit) {
    note?.performModification {
      runWithMuteNotification {
        runnable()
      }
    }
  }

  /**
   * Run section with mute notifications
   */
  override fun runWithMuteNotification(runnable: () -> Unit) {
    val originalNotifyOnChange = notifyOnChange
    notifyOnChange = false
    runnable()
    notifyOnChange = originalNotifyOnChange
  }

  override fun toString(): String {
    val json = asJson().toString()
    val map = BasicNotebookImpl.gson.fromJson<Any>(json, TreeMap::class.java)
    return BasicNotebookImpl.gson.toJson(map)
  }

  private val cachedTextRange = CachedNoteField(::note) {
    var overallLen = 0
    val index = indexInNote
    for (i in 0 until index) {
      overallLen += sourceNotebook.cells[i].source.length + 1
    }
    val end = if (index == sourceNotebook.cells.size - 1)
      overallLen + source.length
    else
      overallLen + source.length + 1
    return@CachedNoteField TextRange(overallLen, end)

  }

  override val textRange: TextRange
    get() = cachedTextRange.getValue()

  override val offset: Int
    get() = textRange.startOffset

  /**
   * Notify parent notebook on content change.
   */
  fun notifyChangeFields(changedFields: List<String>) {
    if (!notifyOnChange || changedFields.isEmpty()) return

    ThreadingAssertions.assertEventDispatchThread()
    assert((note as? BasicNotebookImpl)?.isNoteChangeEnabled == true) {
      "Please edit notebook model just in notebook#performModification section"
    }
    sourceNotebook.addEvent(CellChanged(sourceNotebook, this, changedFields.toSet()))
  }
}