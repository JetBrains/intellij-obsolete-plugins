package com.intellij.bigdatatools.notebooks.core.impl.editor.actions

import com.intellij.bigdatatools.notebooks.utils.NoteMessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent

abstract class NotebookMoveCellAboveAction : NotebookEditorActionBase(NoteMessagesBundle.message("notebook.action.moveCellAbove"), null,
                                                                      AllIcons.General.ArrowUp) {
  override fun actionPerformed(event: AnActionEvent) = service.moveCell(event, NotebookEditorActionService.RelativeDestination.ABOVE)

  override fun update(event: AnActionEvent) {
    super.update(event)
    event.presentation.isEnabledAndVisible = event.presentation.isEnabledAndVisible && service.hasCellAbove(event)
  }
}