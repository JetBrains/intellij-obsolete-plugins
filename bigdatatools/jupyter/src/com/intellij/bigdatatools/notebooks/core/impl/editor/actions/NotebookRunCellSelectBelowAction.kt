package com.intellij.bigdatatools.notebooks.core.impl.editor.actions

import com.intellij.bigdatatools.notebooks.utils.NoteMessagesBundle
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager

abstract class NotebookRunCellSelectBelowAction : NotebookEditorActionBase(NoteMessagesBundle.message("notebook.action.runAndSelectBelow"),
                                                                           NoteMessagesBundle.message(
                                                                             "notebook.action.runAndSelectBelow.descr"), null) {
  override fun actionPerformed(event: AnActionEvent) {
    FileDocumentManager.getInstance().saveAllDocuments()
    service.runCellAndGoBelow(event)
  }

  override fun update(event: AnActionEvent) {
    super.update(event)
    event.presentation.isEnabledAndVisible = event.presentation.isEnabledAndVisible && service.hasCellBelow(event)
  }
}