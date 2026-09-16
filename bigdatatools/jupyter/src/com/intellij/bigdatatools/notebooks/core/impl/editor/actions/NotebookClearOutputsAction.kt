package com.intellij.bigdatatools.notebooks.core.impl.editor.actions

import com.intellij.bigdatatools.jupyter.icons.BigdatatoolsJupyterIcons
import com.intellij.bigdatatools.notebooks.utils.NoteMessagesBundle
import com.intellij.openapi.actionSystem.AnActionEvent

abstract class NotebookClearOutputsAction : NotebookEditorActionBase(NoteMessagesBundle.message("notebook.action.clearAllOutputs"),
                                                                     NoteMessagesBundle.message("notebook.action.clearAllOutputs.descr"),
                                                                     BigdatatoolsJupyterIcons.ClearOutputs) {
  override fun actionPerformed(event: AnActionEvent) {
    service.clearAllOutputs(event)
  }
}