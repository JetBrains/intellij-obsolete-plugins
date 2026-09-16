package com.intellij.bigdatatools.notebooks.core.impl.editor.actions

import com.intellij.bigdatatools.notebooks.utils.NoteMessagesBundle
import com.intellij.openapi.actionSystem.AnActionEvent

abstract class NotebookSelectCellAboveAction : NotebookEditorActionBase(NoteMessagesBundle.message("notebook.action.selectCellAbove"),
                                                                        NoteMessagesBundle.message("notebook.action.selectCellAbove.descr"),
                                                                        null) {
  override fun actionPerformed(event: AnActionEvent) = service.goToPrevCell(event)

  override fun update(event: AnActionEvent) {
    super.update(event)
    event.presentation.isEnabledAndVisible = event.presentation.isEnabledAndVisible && service.hasCellAbove(event)
  }
}