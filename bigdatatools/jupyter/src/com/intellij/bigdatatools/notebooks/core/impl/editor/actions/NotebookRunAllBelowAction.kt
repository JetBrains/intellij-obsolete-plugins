package com.intellij.bigdatatools.notebooks.core.impl.editor.actions

import com.intellij.bigdatatools.notebooks.utils.NoteMessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent

abstract class NotebookRunAllBelowAction : NotebookEditorActionBase(NoteMessagesBundle.message("notebook.action.runAllBelow"),
                                                                    NoteMessagesBundle.message("notebook.action.runAllBelow.descr"),
                                                                    AllIcons.Actions.RunAll) {
  override fun actionPerformed(event: AnActionEvent) = service.runAllBelow(event)

  override fun update(event: AnActionEvent) {
    super.update(event)
    event.presentation.isEnabledAndVisible = event.presentation.isEnabledAndVisible && service.hasCellBelow(event)
  }
}