package com.intellij.bigdatatools.zeppelin.editor.actions.toolbar

import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookEditorActionService.Companion.noteEditor
import com.intellij.bigdatatools.zeppelin.controllers.editor.ZeppelinParagraphFoldingController
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class ZeppelinTogglePresentationAction : DumbAwareAction() {

  private fun getFoldingController(e: AnActionEvent): ZeppelinParagraphFoldingController? = e.noteEditor?.getUserData(
    ZeppelinParagraphFoldingController.FOLDING_CONTROLLER_KEY)

  private fun isSelected(e: AnActionEvent) = getFoldingController(e)?.isCellsHidden ?: false

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isVisible = getFoldingController(e) != null
    if (e.presentation.isVisible) {
      e.presentation.text = if (isSelected(e)) ZepMessagesBundle.message("action.code.blocks.show")
      else ZepMessagesBundle.message("action.BigDataTools.Zeppelin.Toggle.Presentation.text")
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    getFoldingController(e)?.let {
      if (isSelected(e)) {
        it.showAllCells()
      }
      else {
        it.hideAllCells()
      }
    }
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}