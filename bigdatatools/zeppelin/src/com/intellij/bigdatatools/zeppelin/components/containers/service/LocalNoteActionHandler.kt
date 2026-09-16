package com.intellij.bigdatatools.zeppelin.components.containers.service

import com.google.gson.JsonElement
import com.intellij.bigdatatools.notebooks.core.api.editor.NoteEditorActionListener
import com.intellij.bigdatatools.notebooks.core.api.editor.NotebookEditor
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookEditorUtils
import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookUtils
import com.intellij.bigdatatools.notebooks.core.impl.editor.external.ExternalNotebookModifier
import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinCell
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.Messages
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import com.jetbrains.bigdatatools.common.util.invokeLater
import java.util.Date

class LocalNoteActionHandler(private val notebookEditor: NotebookEditor) : Disposable {
  private val noteModifier = ExternalNotebookModifier(notebookEditor.editor)
  private val note = notebookEditor.note
  private val actionListener = object : NoteEditorActionListener {
    override fun addCell(cellIndex: Int, cellText: String) {
      val newCell = note.createCellFromText(cellText)
      newCell.runWithMuteNotification {
        newCell.dateCreated = Date()
      }
      noteModifier.addCell(newCell, cellIndex)
      NotebookEditorUtils.goToCell(notebookEditor.editor, note.cells[cellIndex])
    }

    override fun addCell(cellIndex: Int,
                         cellText: String,
                         isTableHidden: Boolean,
                         isEditorHidden: Boolean,
                         metadata: Map<String, JsonElement>,
                         scrollToCell: Boolean) {
      val newCell = note.createCellFromText(cellText)
      newCell.runWithMuteNotification {
        newCell.dateCreated = Date()
      }

      val zCell = newCell as? ZeppelinCell
      zCell?.runWithMuteNotification {
        newCell.tableHide = isTableHidden
        zCell.editorHide = isEditorHidden
        for ((key, value) in metadata) {
          zCell.setMetadata(key, value)
        }
      }
      noteModifier.addCell(newCell, cellIndex)
      if (scrollToCell) NotebookEditorUtils.goToCell(notebookEditor.editor, note.cells[cellIndex])
    }

    override fun deleteCell(cell: NotebookCell) {
      noteModifier.removeCell(cell)
    }

    override fun moveCell(cell: NotebookCell, toIndex: Int) {
      noteModifier.moveCell(cell, toIndex)
      NotebookEditorUtils.goToCell(notebookEditor.editor, cell)
    }

    override fun cloneCell(cell: NotebookCell, index: Int) {
      noteModifier.addCell(cell.copy(), index)
      val notebook = cell.note ?: return
      NotebookEditorUtils.goToCell(notebookEditor.editor, notebook.cells[index])
    }

    override fun splitCell(cell: NotebookCell, lineOffset: Int) {
      NotebookUtils.splitCell(notebookEditor, cell, lineOffset)
    }

    override fun mergeWithNext(cell: NotebookCell) {
      NotebookUtils.mergeCellWithNext(notebookEditor, cell)
    }

    override fun runAll() = showIsNoConfigIfRequired()
    override fun stopAll() = showIsNoConfigIfRequired()
    override fun runCell(cell: NotebookCell) = showIsNoConfigIfRequired()
    override fun stopCell(cell: NotebookCell) = showIsNoConfigIfRequired()
    override fun runAllBelow(cell: NotebookCell) = showIsNoConfigIfRequired()
    override fun runAllAbove(cell: NotebookCell) = showIsNoConfigIfRequired()
    override fun runCellGoBelow(cell: NotebookCell) = showIsNoConfigIfRequired()
    override fun restartInterpreter(cell: NotebookCell) = showIsNoConfigIfRequired()

    override fun clearAllOutput() = note.cells.forEach {
      noteModifier.setOutput(it, null)
    }

    override fun clearCellOutput(cell: NotebookCell) {
      noteModifier.setOutput(cell, null)
    }
  }

  init {
    notebookEditor.addActionListener(actionListener)
  }

  override fun dispose() {
    notebookEditor.removeActionListener(actionListener)
  }

  private fun showIsNoConfigIfRequired() {
    if (isNoConfig())
      showNoConfigMessage()
  }

  private fun isNoConfig(): Boolean {
    val configId = NotebookFileUtil.getConfigId(notebookEditor.file) ?: return true
    return DriverManager.getDriverById(notebookEditor.project, configId) == null
  }

  private fun showNoConfigMessage() = invokeLater {
    val title = ZepMessagesBundle.message("note.local.action.error.title")
    val message = ZepMessagesBundle.message("note.local.no.config.error.message")
    Messages.showErrorDialog(notebookEditor.project, message, title)
  }
}