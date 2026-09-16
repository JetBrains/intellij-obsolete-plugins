package com.intellij.bigdatatools.notebooks.core.impl.editor.actions

import com.intellij.bigdatatools.notebooks.utils.NoteMessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent

abstract class NotebookMergeCellAboveAction : NotebookEditorActionBase(NoteMessagesBundle.message("notebook.action.merge.cell"), null,
                                                                       AllIcons.Actions.Collapseall) {
  override fun actionPerformed(event: AnActionEvent) = service.mergeCellWithNext(event)

  override fun update(event: AnActionEvent) {
    super.update(event)
    event.presentation.isEnabledAndVisible = event.presentation.isEnabledAndVisible && service.hasCellBelow(event)
  }
}