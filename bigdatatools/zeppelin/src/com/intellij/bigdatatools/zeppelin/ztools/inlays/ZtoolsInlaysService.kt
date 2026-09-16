package com.intellij.bigdatatools.zeppelin.ztools.inlays

import com.google.gson.Gson
import com.intellij.bigdatatools.notebooks.core.api.editor.NoteEditorActionListener
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellChanged
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookChangeListener
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookEvent
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookSchema
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.CellResultType
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.ztools.controller.ZtoolsNoteController.Companion.isDebugCell
import com.intellij.bigdatatools.zeppelin.ztools.dataframe.SparkDataFrameSchema
import com.intellij.bigdatatools.zeppelin.ztools.dataframe.ZtoolsDataFrameUtils
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Key

class ZtoolsInlaysService(val editor: ZeppelinEditor) : Disposable {
  private val note = editor.note

  private val noteChangeListener = object : NotebookChangeListener {
    override fun onEvent(notebookEvent: NotebookEvent) {
      if (notebookEvent !is CellChanged)
        return
      if (NotebookSchema.results !in notebookEvent.changedFields)
        return
      if (!notebookEvent.cell.status.isFinished)
        return

      val dataFrames = ZtoolsDataFrameUtils.getDataFramesForFile(editor.file)
      updateZtoolsInlays(notebookEvent.cell, dataFrames)
    }

  }

  private fun updateZtoolsInlays(cell: NotebookCell, dataFrames: Map<String, SparkDataFrameSchema>) {
    val oldDfInfo = cell.getMetadata(META_DATAFRAMES)

    val newDfNames = findCellDfOutput(cell)
    val newDataframes = newDfNames.mapNotNull { dataFrames[it] }
    val newDfJson = if (newDataframes.isNotEmpty())
      Gson().toJsonTree(newDataframes)
    else
      null

    if (oldDfInfo == newDfJson) {
      return
    }

    note.performModification {
      newDfJson?.let { cell.setMetadata(META_DATAFRAMES, it) } ?: cell.removeMetadata(META_DATAFRAMES)
    }
  }

  private val actionListener = object : NoteEditorActionListener {
    override fun clearAllOutput() {
      note.performModification {
        note.cells.forEach {
          it.removeMetadata(META_DATAFRAMES)
        }
      }
    }

    override fun clearCellOutput(cell: NotebookCell) {
      note.performModification {
        cell.removeMetadata(META_DATAFRAMES)
      }
    }
  }

  init {
    editor.putUserData(KEY, this)
    editor.addActionListener(actionListener)
    note.addNotebookChangeListener(noteChangeListener)
  }

  override fun dispose() {
    editor.putUserData(KEY, null)
    note.removeNotebookChangeListener(noteChangeListener)
    editor.removeActionListener(actionListener)
  }

  fun invokeUpdate() {
    val dataFrames = ZtoolsDataFrameUtils.getDataFramesForFile(editor.file)
    note.cells.filter { !it.isDebugCell }.forEach {
      updateZtoolsInlays(it, dataFrames)
    }
  }

  private fun findCellDfOutput(cell: NotebookCell): List<String> {
    val outputs = cell.output?.msg?.filter { it.type == CellResultType.TEXT }?.map { it.data } ?: emptyList()
    if (outputs.isEmpty())
      return emptyList()
    val regex = Regex("##!!%%.*%%##!!")

    val prefix = "\u001B[1m\u001B[34m"
    val suffix = "\u001B[0m: \u001B[1m\u001B[32m!#!#\u001B[0m"

    val customPrefix = "##!!%%"
    val customSuffix = "%%##!!"
    var totalRes = outputs.joinToString(separator = "\n")

    ZtoolsDataFrameUtils.datasetTypes.forEach {
      totalRes = totalRes.replace(it, "!#!#")
    }
    totalRes = totalRes.replace(prefix, customPrefix).replace(suffix, customSuffix)
    return regex.findAll(totalRes).map { it.value.removePrefix(customPrefix).removeSuffix(customSuffix) }.toList()

  }

  companion object {
    private const val META_DATAFRAMES = "ZTOOLS_DATA_FRAMES"
    private val KEY = Key<ZtoolsInlaysService>("ZtoolsInlaysService")

    fun getFor(editor: ZeppelinEditor) = editor.getUserData(KEY)
  }
}