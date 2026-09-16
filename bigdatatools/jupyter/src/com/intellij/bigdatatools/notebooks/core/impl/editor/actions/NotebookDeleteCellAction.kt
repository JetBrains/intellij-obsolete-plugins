package com.intellij.bigdatatools.notebooks.core.impl.editor.actions

import com.intellij.bigdatatools.notebooks.utils.NoteMessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent

abstract class NotebookDeleteCellAction : NotebookEditorActionBase(NoteMessagesBundle.message("notebook.action.deleteCell"),
                                                                   NoteMessagesBundle.message("notebook.action.deleteCell.descr"),
                                                                   AllIcons.Actions.DeleteTag) {
  override fun actionPerformed(event: AnActionEvent) {
    service.deleteCell(event)
  }
}