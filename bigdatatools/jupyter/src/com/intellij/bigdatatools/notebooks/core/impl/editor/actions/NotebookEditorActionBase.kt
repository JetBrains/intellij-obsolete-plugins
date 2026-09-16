package com.intellij.bigdatatools.notebooks.core.impl.editor.actions

import com.intellij.bigdatatools.notebooks.core.impl.psi.NotebookPsiFile
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.NlsActions
import javax.swing.Icon

abstract class NotebookEditorActionBase : DumbAwareAction {

  constructor() {
    this.service = NotebookEditorActionService()
  }

  constructor(@NlsActions.ActionText text: String?, @NlsActions.ActionDescription description: String?, icon: Icon?) :
    super(text, description, icon) {
    this.service = NotebookEditorActionService()
  }

  protected val service: NotebookEditorActionService

  override fun update(event: AnActionEvent) {
    event.presentation.isEnabledAndVisible = event.getData(CommonDataKeys.PSI_FILE) is NotebookPsiFile
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}