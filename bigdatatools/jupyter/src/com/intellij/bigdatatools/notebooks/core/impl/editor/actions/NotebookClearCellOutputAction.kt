package com.intellij.bigdatatools.notebooks.core.impl.editor.actions

import com.intellij.bigdatatools.jupyter.icons.BigdatatoolsJupyterIcons
import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookEditorActionService.Companion.noteCell
import com.intellij.bigdatatools.notebooks.utils.NoteMessagesBundle
import com.intellij.openapi.actionSystem.AnActionEvent

abstract class NotebookClearCellOutputAction : NotebookEditorActionBase(NoteMessagesBundle.message("notebook.action.clearOutput"), null,
                                                                        BigdatatoolsJupyterIcons.ClearOutputs) {
  override fun actionPerformed(event: AnActionEvent) {
    service.clearCellOutput(event)
  }

  override fun update(event: AnActionEvent) {
    super.update(event)
    event.presentation.isEnabledAndVisible = event.presentation.isEnabledAndVisible && event.noteCell?.output != null
  }
}