package com.intellij.bigdatatools.notebooks.core.impl.editor.actions

import com.intellij.bigdatatools.notebooks.utils.NoteMessagesBundle
import com.intellij.openapi.actionSystem.AnActionEvent

abstract class NotebookInsertCellBelowAction : NotebookEditorActionBase(NoteMessagesBundle.message("notebook.action.insertCellBelow"), null,
                                                                        null) {
  override fun actionPerformed(event: AnActionEvent) = service.insertNewEmptyCell(event,
                                                                                  NotebookEditorActionService.RelativeDestination.BELOW)
}