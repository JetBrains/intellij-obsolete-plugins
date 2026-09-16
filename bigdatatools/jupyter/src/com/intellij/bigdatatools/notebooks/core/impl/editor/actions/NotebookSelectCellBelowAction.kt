package com.intellij.bigdatatools.notebooks.core.impl.editor.actions

import com.intellij.bigdatatools.notebooks.utils.NoteMessagesBundle
import com.intellij.openapi.actionSystem.AnActionEvent

abstract class NotebookSelectCellBelowAction : NotebookEditorActionBase(NoteMessagesBundle.message("notebook.action.selectCellBelow"),
                                                                        NoteMessagesBundle.message("notebook.action.selectCellBelow.descr"),
                                                                        null) {
  override fun actionPerformed(event: AnActionEvent) = service.goToNextCell(event)

  override fun update(event: AnActionEvent) {
    super.update(event)
    event.presentation.isEnabledAndVisible = event.presentation.isEnabledAndVisible && service.hasCellBelow(event)
  }
}