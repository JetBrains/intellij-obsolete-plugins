package com.intellij.bigdatatools.notebooks.core.impl.editor.actions

import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellStatus
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookEditorActionService.Companion.noteCell
import com.intellij.bigdatatools.notebooks.utils.NoteMessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager

abstract class NotebookStopCellAction : NotebookEditorActionBase(NoteMessagesBundle.message("notebook.action.stop"), null,
                                                                 AllIcons.Actions.Pause) {
  override fun actionPerformed(event: AnActionEvent) {
    FileDocumentManager.getInstance().saveAllDocuments()
    service.stopCell(event)
  }

  override fun update(event: AnActionEvent) {
    super.update(event)
    val cell = event.noteCell
    event.presentation.isEnabledAndVisible = event.presentation.isEnabledAndVisible &&
                                             (cell?.status == CellStatus.PENDING || cell?.status == CellStatus.RUNNING)
  }
}