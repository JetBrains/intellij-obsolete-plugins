package com.intellij.bigdatatools.notebooks.core.impl.editor.actions

import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellStatus
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookEditorActionService.Companion.noteCell
import com.intellij.bigdatatools.notebooks.utils.NoteMessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent

abstract class NotebookRunCellAction : NotebookEditorActionBase(NoteMessagesBundle.message("notebook.action.run"), null,
                                                                AllIcons.Actions.Execute) {
  override fun update(event: AnActionEvent) {
    super.update(event)
    val cell = event.noteCell
    event.presentation.isEnabledAndVisible = event.presentation.isEnabledAndVisible &&
                                             cell != null &&
                                             cell.status != CellStatus.PENDING &&
                                             cell.status != CellStatus.RUNNING
  }
}