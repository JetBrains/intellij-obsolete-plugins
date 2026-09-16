package com.intellij.bigdatatools.notebooks.core.impl.editor.actions

import com.intellij.bigdatatools.notebooks.utils.NoteMessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent

abstract class NotebookCloneCellAction : NotebookEditorActionBase(NoteMessagesBundle.message("notebook.action.cloneCell"), null,
                                                                  AllIcons.Actions.Copy) {
  override fun actionPerformed(event: AnActionEvent) {
    service.cloneCell(event)
  }
}