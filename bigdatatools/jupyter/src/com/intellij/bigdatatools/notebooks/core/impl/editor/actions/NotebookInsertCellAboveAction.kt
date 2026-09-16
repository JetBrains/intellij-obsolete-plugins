package com.intellij.bigdatatools.notebooks.core.impl.editor.actions

import com.intellij.bigdatatools.notebooks.utils.NoteMessagesBundle
import com.intellij.openapi.actionSystem.AnActionEvent

abstract class NotebookInsertCellAboveAction : NotebookEditorActionBase(NoteMessagesBundle.message("notebook.action.insertCellAbove"), null,
                                                                        null) {
  override fun actionPerformed(event: AnActionEvent) = service.insertNewEmptyCell(event,
                                                                                  NotebookEditorActionService.RelativeDestination.ABOVE)
}